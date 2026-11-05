package com.torneobjj.persistencia;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.torneobjj.llave.Llave;
import com.torneobjj.llave.Ronda;
import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.DivisionEdad;
import com.torneobjj.modelo.Marcador;
import com.torneobjj.modelo.Nivel;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Resultado;
import com.torneobjj.modelo.Sexo;
import com.torneobjj.modelo.TipoVictoria;
import com.torneobjj.servicio.Torneo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda y abre torneos en archivos JSON.
 *
 * <p>No se guardan las llaves armadas: se guardan los competidores (con su peso), si el torneo
 * estaba sorteado y los resultados en el orden en que se cargaron. Al abrir, se vuelve a sortear
 * (el sorteo es determinístico) y se repiten los resultados. Así el archivo es simple de leer
 * y no puede quedar una llave inconsistente.
 */
public final class ArchivoTorneo {

    public static final String EXTENSION = ".torneo.json";
    static final int VERSION = 1;

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ArchivoTorneo() {
    }

    // ------------------------------------------------------------ formato del archivo

    record Datos(int version, String nombre, LocalDate fecha, boolean sorteado,
                 List<CompetidorGuardado> competidores, List<ResultadoGuardado> resultados) {
    }

    record CompetidorGuardado(String dni, String nombre, String apellido, LocalDate nacimiento,
                              Sexo sexo, Nivel nivel, CategoriaPeso categoriaPeso, Double peso) {
    }

    /** Resultado de un combate, ubicado por categoría, ronda y posición dentro de la ronda. */
    record ResultadoGuardado(Sexo sexo, DivisionEdad division, CategoriaPeso peso, Nivel nivel,
                             int ronda, int combate, String dni1, String dni2,
                             TipoVictoria tipo, String dniGanador, Marcador marcador1, Marcador marcador2) {
    }

    // ------------------------------------------------------------ guardar

    public static void guardar(Torneo torneo, Path archivo) throws IOException {
        List<CompetidorGuardado> competidores = new ArrayList<>();
        for (Participante p : torneo.getParticipantes()) {
            competidores.add(new CompetidorGuardado(p.getDni(), p.getNombre(), p.getApellido(),
                    p.getFechaNacimiento(), p.getSexo(), p.getNivel(), p.getCategoriaPeso(), p.getPesoOficial()));
        }
        List<ResultadoGuardado> resultados = new ArrayList<>();
        for (Llave llave : torneo.getLlaves().values()) {
            var cat = llave.getCategoria();
            List<Ronda> rondas = llave.getRondas();
            for (int i = 0; i < rondas.size(); i++) {
                List<Combate> combates = rondas.get(i).getCombates();
                for (int j = 0; j < combates.size(); j++) {
                    Combate c = combates.get(j);
                    if (c.estaTerminado()) {
                        Resultado r = c.getResultado();
                        resultados.add(new ResultadoGuardado(cat.sexo(), cat.division(), cat.peso(), cat.nivel(),
                                i, j, c.getCompetidor1().getDni(), c.getCompetidor2().getDni(),
                                r.tipo(), r.ganador().getDni(), r.marcador1(), r.marcador2()));
                    }
                }
            }
        }
        Datos datos = new Datos(VERSION, torneo.getNombre(), torneo.getFecha(), torneo.estaSorteado(),
                competidores, resultados);

        // Se escribe primero a un temporal: si algo falla, el archivo anterior queda intacto
        Path absoluto = archivo.toAbsolutePath();
        Path temporal = Files.createTempFile(absoluto.getParent(), "torneo", ".tmp");
        try {
            JSON.writeValue(temporal.toFile(), datos);
            Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    // ------------------------------------------------------------ abrir

    /**
     * Abre un torneo guardado.
     *
     * @throws IOException si no se puede leer el archivo o su contenido no es un torneo válido
     *                     (el mensaje está pensado para mostrárselo al usuario)
     */
    public static Torneo abrir(Path archivo) throws IOException {
        byte[] contenido;
        try {
            contenido = Files.readAllBytes(archivo);
        } catch (NoSuchFileException e) {
            throw new IOException("No existe el archivo " + archivo.getFileName(), e);
        }
        Datos datos;
        try {
            datos = JSON.readValue(contenido, Datos.class);
        } catch (JacksonException e) {
            throw new IOException("El archivo no es un torneo válido o está dañado", e);
        }
        if (datos.version() > VERSION) {
            throw new IOException("El archivo es de una versión más nueva del programa (versión "
                    + datos.version() + ")");
        }
        try {
            return reconstruir(datos);
        } catch (RuntimeException e) {
            throw new IOException("El archivo tiene datos inválidos: " + e.getMessage(), e);
        }
    }

    private static Torneo reconstruir(Datos datos) {
        Torneo torneo = new Torneo(datos.nombre(), datos.fecha());
        for (CompetidorGuardado c : lista(datos.competidores())) {
            Participante p = new Participante(c.dni(), c.nombre(), c.apellido(), c.nacimiento(),
                    c.sexo(), c.nivel(), c.categoriaPeso());
            torneo.inscribir(p);
            if (c.peso() != null) {
                torneo.registrarPeso(p, c.peso());
            }
        }
        if (!datos.sorteado()) {
            if (!lista(datos.resultados()).isEmpty()) {
                throw new IllegalArgumentException("hay resultados pero el torneo no está sorteado");
            }
            return torneo;
        }
        torneo.sortear();

        // Los resultados se guardaron por llave y en orden de ronda, así que se pueden repetir en orden:
        // cada ronda aparece recién cuando terminó la anterior.
        for (ResultadoGuardado r : lista(datos.resultados())) {
            var categoria = new com.torneobjj.modelo.Categoria(r.sexo(), r.division(), r.peso(), r.nivel());
            Llave llave = torneo.getLlaves().get(categoria);
            if (llave == null) {
                throw new IllegalArgumentException("no existe la categoría " + categoria);
            }
            List<Ronda> rondas = llave.getRondas();
            if (r.ronda() >= rondas.size() || r.combate() >= rondas.get(r.ronda()).getCombates().size()) {
                throw new IllegalArgumentException("combate inexistente en " + categoria);
            }
            Combate combate = rondas.get(r.ronda()).getCombates().get(r.combate());
            if (!combate.getCompetidor1().getDni().equals(r.dni1()) || !combate.getCompetidor2().getDni().equals(r.dni2())) {
                throw new IllegalArgumentException("el combate " + combate + " no coincide con el guardado");
            }
            Participante ganador = combate.getCompetidor1().getDni().equals(r.dniGanador())
                    ? combate.getCompetidor1()
                    : combate.getCompetidor2().getDni().equals(r.dniGanador()) ? combate.getCompetidor2() : null;
            if (ganador == null) {
                throw new IllegalArgumentException("el ganador de " + combate + " no pelea en ese combate");
            }
            combate.registrar(new Resultado(ganador, r.tipo(), r.marcador1(), r.marcador2()));
        }
        return torneo;
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }

    /** Agrega la extensión si el usuario no la escribió. */
    public static Path conExtension(Path archivo) {
        String nombre = archivo.getFileName().toString();
        if (nombre.endsWith(EXTENSION)) {
            return archivo;
        }
        if (nombre.endsWith(".json")) {
            nombre = nombre.substring(0, nombre.length() - ".json".length());
        }
        return archivo.resolveSibling(nombre + EXTENSION);
    }
}
