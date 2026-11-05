package com.torneobjj.servicio;

import com.torneobjj.llave.Llave;
import com.torneobjj.llave.Sorteo;
import com.torneobjj.modelo.Categoria;
import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.DivisionEdad;
import com.torneobjj.modelo.Participante;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Estado completo del torneo y punto de entrada para la interfaz.
 * Flujo: inscripción → pesaje → sorteo → combates.
 * Una vez sorteado no se puede inscribir ni pesar (el sorteo usa los pesos).
 */
public class Torneo {

    private final String nombre;
    private final LocalDate fecha;
    private final List<Participante> participantes = new ArrayList<>();
    private Map<Categoria, Llave> llaves = new LinkedHashMap<>();
    private boolean sorteado;

    public Torneo(String nombre, LocalDate fecha) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El torneo necesita un nombre");
        }
        this.nombre = nombre.trim();
        this.fecha = Objects.requireNonNull(fecha);
    }

    // ------------------------------------------------------------ inscripción

    public void inscribir(Participante p) {
        verificarSinSortear("inscribir");
        if (buscarPorDni(p.getDni()).isPresent()) {
            throw new IllegalArgumentException("Ya hay un competidor inscripto con DNI " + p.getDni());
        }
        if (p.getFechaNacimiento().isAfter(fecha)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser posterior al torneo");
        }
        int edad = p.getEdadEnAnio(getAnio());
        if (DivisionEdad.desdeEdad(edad).isEmpty()) {
            throw new IllegalArgumentException("Tiene " + edad + " años en " + getAnio()
                    + "; la edad mínima es " + DivisionEdad.EDAD_MINIMA);
        }
        participantes.add(p);
    }

    public void eliminar(Participante p) {
        verificarSinSortear("eliminar competidores");
        participantes.remove(p);
    }

    // ------------------------------------------------------------ pesaje

    public void registrarPeso(Participante p, double kg) {
        verificarSinSortear("registrar pesos");
        if (!participantes.contains(p)) {
            throw new IllegalArgumentException(p + " no está inscripto");
        }
        p.registrarPeso(kg);
    }

    public long cantidadPesados() {
        return participantes.stream().filter(Participante::estaPesado).count();
    }

    // ------------------------------------------------------------ sorteo

    public void sortear() {
        verificarSinSortear("sortear");
        if (cantidadPesados() == 0) {
            throw new IllegalStateException("No hay competidores pesados para sortear");
        }
        llaves = Sorteo.sortear(participantes, getAnio());
        sorteado = true;
    }

    /** Deshace el sorteo para volver a inscribir o pesar. Solo si no se cargó ningún resultado. */
    public void anularSorteo() {
        if (hayResultadosCargados()) {
            throw new IllegalStateException("No se puede anular el sorteo: ya hay resultados cargados");
        }
        llaves = new LinkedHashMap<>();
        sorteado = false;
    }

    public boolean estaSorteado() {
        return sorteado;
    }

    public boolean hayResultadosCargados() {
        return llaves.values().stream()
                .flatMap(l -> l.getRondas().stream())
                .flatMap(r -> r.getCombates().stream())
                .anyMatch(Combate::estaTerminado);
    }

    /** Competidores inscriptos que no se pesaron y por eso quedaron fuera del sorteo. */
    public List<Participante> getAusentes() {
        return participantes.stream().filter(p -> !p.estaPesado()).toList();
    }

    public Map<Categoria, Llave> getLlaves() {
        return Collections.unmodifiableMap(llaves);
    }

    public Optional<Llave> llaveDe(Participante p) {
        return llaves.values().stream().filter(l -> l.contiene(p)).findFirst();
    }

    // ------------------------------------------------------------ búsqueda

    public Optional<Participante> buscarPorDni(String dni) {
        String buscado = dni == null ? "" : dni.trim();
        return participantes.stream().filter(p -> p.getDni().equalsIgnoreCase(buscado)).findFirst();
    }

    /**
     * Busca por nombre, apellido o DNI, sin importar mayúsculas ni acentos.
     * Con varias palabras, tienen que coincidir todas ("juan per" encuentra a Juan Pérez).
     */
    public List<Participante> buscar(String texto) {
        String[] palabras = normalizar(texto).split("\\s+");
        return participantes.stream()
                .filter(p -> {
                    String datos = normalizar(p.getNombreCompleto() + " " + p.getDni());
                    for (String palabra : palabras) {
                        if (!datos.contains(palabra)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    // ------------------------------------------------------------ getters

    public String getNombre() { return nombre; }
    public LocalDate getFecha() { return fecha; }
    public int getAnio() { return fecha.getYear(); }
    public List<Participante> getParticipantes() { return Collections.unmodifiableList(participantes); }

    private void verificarSinSortear(String accion) {
        if (sorteado) {
            throw new IllegalStateException("No se puede " + accion + ": las llaves ya fueron sorteadas");
        }
    }
}
