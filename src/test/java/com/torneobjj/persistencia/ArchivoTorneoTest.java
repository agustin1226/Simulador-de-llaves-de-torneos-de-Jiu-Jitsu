package com.torneobjj.persistencia;

import static org.junit.jupiter.api.Assertions.*;

import com.torneobjj.llave.Llave;
import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Marcador;
import com.torneobjj.modelo.Participante;
import com.torneobjj.servicio.DatosDePrueba;
import com.torneobjj.servicio.Torneo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchivoTorneoTest {

    @TempDir
    Path carpeta;

    private static Torneo torneoDePrueba(long semilla) {
        Torneo t = new Torneo("Copa de prueba", LocalDate.of(2026, 11, 20));
        DatosDePrueba.cargar(t, 70, new Random(semilla));
        return t;
    }

    /** Carga resultados variados: sumisión, puntos, ventajas, decisión del árbitro, y deja llaves a medias. */
    private static void cargarResultados(Torneo t, Random random) {
        int n = 0;
        for (Llave llave : t.getLlaves().values()) {
            int combatesACargar = random.nextInt(12);
            for (int k = 0; k < combatesACargar && !llave.estaTerminada(); k++) {
                Combate c = llave.getCombatesPendientes().get(0);
                switch (n++ % 4) {
                    case 0 -> c.registrarSumision(random.nextBoolean() ? c.getCompetidor1() : c.getCompetidor2());
                    case 1 -> c.registrarPorPuntos(new Marcador(random.nextInt(9), 0, 0), new Marcador(random.nextInt(9) + 9, 1, 0));
                    case 2 -> c.registrarPorPuntos(new Marcador(2, 2, 0), new Marcador(2, 1, 1));
                    default -> {
                        Marcador m = new Marcador(0, 0, 0);
                        if (c.requiereDecisionArbitro(m, m)) {
                            c.registrarDecisionArbitro(m, m, c.getCompetidor2());
                        } else {
                            c.registrarPorPuntos(m, m);
                        }
                    }
                }
            }
        }
    }

    private static String resumen(Torneo t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getNombre()).append(t.getFecha()).append(t.estaSorteado());
        for (Participante p : t.getParticipantes()) {
            sb.append(p.getDni()).append(p.getNombreCompleto()).append(p.getFechaNacimiento()).append(p.getSexo())
                    .append(p.getNivel()).append(p.getCategoriaPeso()).append(p.getPesoOficial());
        }
        t.getLlaves().forEach((cat, llave) -> {
            sb.append(cat);
            llave.getRondas().forEach(r -> r.getCombates().forEach(c -> sb.append(c).append(c.getResultado())));
            sb.append(llave.getPodio());
        });
        return sb.toString().replaceAll("@[0-9a-f]+", "");
    }

    @Test
    void guardaYAbreUnTorneoSinSortear() throws IOException {
        Torneo original = torneoDePrueba(3);
        Path archivo = carpeta.resolve("t" + ArchivoTorneo.EXTENSION);
        ArchivoTorneo.guardar(original, archivo);

        Torneo abierto = ArchivoTorneo.abrir(archivo);
        assertFalse(abierto.estaSorteado());
        assertEquals(resumen(original), resumen(abierto));
    }

    @Test
    void guardaYAbreUnTorneoConResultados() throws IOException {
        for (long semilla = 1; semilla <= 8; semilla++) {
            Torneo original = torneoDePrueba(semilla);
            original.sortear();
            cargarResultados(original, new Random(semilla));
            Path archivo = carpeta.resolve("t" + semilla + ArchivoTorneo.EXTENSION);
            ArchivoTorneo.guardar(original, archivo);

            Torneo abierto = ArchivoTorneo.abrir(archivo);
            assertTrue(abierto.estaSorteado());
            assertEquals(resumen(original), resumen(abierto), "semilla " + semilla);
            assertTrue(abierto.hayResultadosCargados());
        }
    }

    @Test
    void elArchivoEsLegible() throws IOException {
        Torneo t = new Torneo("Copa", LocalDate.of(2026, 3, 1));
        Participante p = new Participante("123", "Juan", "Pérez", LocalDate.of(1990, 5, 4),
                com.torneobjj.modelo.Sexo.MASCULINO, com.torneobjj.modelo.Nivel.AMATEUR,
                com.torneobjj.modelo.CategoriaPeso.MEDIO);
        t.inscribir(p);
        t.registrarPeso(p, 80.5);
        Path archivo = carpeta.resolve("legible.torneo.json");
        ArchivoTorneo.guardar(t, archivo);

        String json = Files.readString(archivo);
        assertTrue(json.contains("\"fecha\" : \"2026-03-01\""), json);
        assertTrue(json.contains("\"apellido\" : \"Pérez\""), json);
        assertTrue(json.contains("\"categoriaPeso\" : \"MEDIO\""), json);
        assertTrue(json.contains("\"peso\" : 80.5"), json);
    }

    @Test
    void sobrescribeUnArchivoExistente() throws IOException {
        Path archivo = carpeta.resolve("t.torneo.json");
        Torneo t = torneoDePrueba(1);
        ArchivoTorneo.guardar(t, archivo);
        t.setNombre("Nombre nuevo");
        ArchivoTorneo.guardar(t, archivo);
        assertEquals("Nombre nuevo", ArchivoTorneo.abrir(archivo).getNombre());
        try (var archivos = Files.list(carpeta)) {
            assertEquals(List.of(archivo), archivos.toList(), "No quedan temporales");
        }
    }

    @Test
    void erroresConMensajeClaro() throws IOException {
        IOException noExiste = assertThrows(IOException.class, () -> ArchivoTorneo.abrir(carpeta.resolve("nada.json")));
        assertTrue(noExiste.getMessage().contains("No existe"), noExiste.getMessage());

        Path roto = carpeta.resolve("roto.json");
        Files.writeString(roto, "{ esto no es json");
        IOException danado = assertThrows(IOException.class, () -> ArchivoTorneo.abrir(roto));
        assertTrue(danado.getMessage().contains("dañado"), danado.getMessage());

        Path futuro = carpeta.resolve("futuro.json");
        Files.writeString(futuro, "{\"version\": 99, \"nombre\": \"X\", \"fecha\": \"2026-01-01\"}");
        IOException version = assertThrows(IOException.class, () -> ArchivoTorneo.abrir(futuro));
        assertTrue(version.getMessage().contains("versión más nueva"), version.getMessage());

        Path menor = carpeta.resolve("menor.json");
        Files.writeString(menor, """
                {"version": 1, "nombre": "X", "fecha": "2026-01-01", "sorteado": false,
                 "competidores": [{"dni": "1", "nombre": "A", "apellido": "B", "nacimiento": "2015-01-01",
                                   "sexo": "MASCULINO", "nivel": "AMATEUR", "categoriaPeso": "GALLO"}]}
                """);
        IOException invalido = assertThrows(IOException.class, () -> ArchivoTorneo.abrir(menor));
        assertTrue(invalido.getMessage().contains("datos inválidos"), invalido.getMessage());
    }

    @Test
    void resultadoManipuladoSeDetecta() throws IOException {
        Torneo t = torneoDePrueba(2);
        t.sortear();
        Llave llave = t.getLlaves().values().stream().filter(l -> !l.getCombatesPendientes().isEmpty()).findFirst().orElseThrow();
        Combate c = llave.getCombatesPendientes().get(0);
        c.registrarPorPuntos(new Marcador(4, 0, 0), new Marcador(2, 0, 0));
        Path archivo = carpeta.resolve("t.torneo.json");
        ArchivoTorneo.guardar(t, archivo);

        // Se cambia el ganador en el archivo sin cambiar el marcador
        String json = Files.readString(archivo).replace(
                "\"dniGanador\" : \"" + c.getCompetidor1().getDni() + "\"",
                "\"dniGanador\" : \"" + c.getCompetidor2().getDni() + "\"");
        Files.writeString(archivo, json);
        IOException e = assertThrows(IOException.class, () -> ArchivoTorneo.abrir(archivo));
        assertTrue(e.getMessage().contains("no coincide con su marcador"), e.getMessage());
    }

    @Test
    void agregaLaExtension() {
        assertEquals(Path.of("copa.torneo.json"), ArchivoTorneo.conExtension(Path.of("copa")));
        assertEquals(Path.of("copa.torneo.json"), ArchivoTorneo.conExtension(Path.of("copa.json")));
        assertEquals(Path.of("copa.torneo.json"), ArchivoTorneo.conExtension(Path.of("copa.torneo.json")));
    }
}
