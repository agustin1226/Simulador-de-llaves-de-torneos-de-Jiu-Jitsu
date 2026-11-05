package com.torneobjj.llave;

import com.torneobjj.modelo.Categoria;
import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Participante;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Llave de una categoría. Se arma ronda por ronda: la siguiente ronda se genera
 * sola cuando todos los combates de la actual tienen resultado.
 *
 * <ul>
 *   <li>1 competidor: campeón por W.O., sin combates.</li>
 *   <li>2: final directa.</li>
 *   <li>3: triangular. Los dos más pesados (A y B) pelean; el perdedor pelea contra
 *       el más liviano (C); la final es entre el ganador de A-B y el ganador del segundo combate.</li>
 *   <li>4 o más: en cada ronda se ordena por peso y pelean los consecutivos. Si son impares,
 *       pasa libre el más liviano (sin repetir el pase libre de la ronda anterior).
 *       Si en alguna ronda quedan 3, se pasa al triangular.</li>
 * </ul>
 */
public class Llave {

    /** Del más liviano al más pesado; a igual peso, por DNI para que el sorteo sea reproducible. */
    static final Comparator<Participante> POR_PESO = Comparator
            .comparingDouble(Participante::getPesoOficial)
            .thenComparing(Participante::getDni);

    private final Categoria categoria;
    private final List<Participante> participantes;
    private final List<Ronda> rondas = new ArrayList<>();
    /** Estado del triangular; null si la llave no llegó a 3 competidores. */
    private Triangular triangular;

    private Participante campeon;
    private Participante subcampeon;
    private final List<Participante> terceros = new ArrayList<>();

    public Llave(Categoria categoria, List<Participante> participantes) {
        this.categoria = Objects.requireNonNull(categoria);
        if (participantes == null || participantes.isEmpty()) {
            throw new IllegalArgumentException("La llave necesita al menos un competidor");
        }
        if (new HashSet<>(participantes).size() != participantes.size()) {
            throw new IllegalArgumentException("Hay competidores repetidos");
        }
        for (Participante p : participantes) {
            if (!p.estaPesado()) {
                throw new IllegalArgumentException(p + " no pasó por el pesaje");
            }
        }
        List<Participante> ordenados = new ArrayList<>(participantes);
        ordenados.sort(POR_PESO);
        this.participantes = List.copyOf(ordenados);
        avanzar();
    }

    // ----------------------------------------------------------------- consultas

    public Categoria getCategoria() {
        return categoria;
    }

    /** Competidores de la llave, del más liviano al más pesado. */
    public List<Participante> getParticipantes() {
        return participantes;
    }

    public List<Ronda> getRondas() {
        avanzar();
        return Collections.unmodifiableList(rondas);
    }

    /** Combates que se pueden pelear ahora (los de la ronda actual sin resultado). */
    public List<Combate> getCombatesPendientes() {
        avanzar();
        if (rondas.isEmpty()) {
            return List.of();
        }
        return rondas.get(rondas.size() - 1).getCombates().stream()
                .filter(c -> !c.estaTerminado())
                .toList();
    }

    public boolean estaTerminada() {
        avanzar();
        return campeon != null;
    }

    public Optional<Participante> getCampeon() {
        avanzar();
        return Optional.ofNullable(campeon);
    }

    /** Podio final; vacío si la llave no terminó. */
    public List<Puesto> getPodio() {
        avanzar();
        List<Puesto> podio = new ArrayList<>();
        if (campeon == null) {
            return podio;
        }
        podio.add(new Puesto(1, campeon));
        if (subcampeon != null) {
            podio.add(new Puesto(2, subcampeon));
        }
        for (Participante p : terceros) {
            podio.add(new Puesto(3, p));
        }
        return podio;
    }

    public boolean contiene(Participante p) {
        return participantes.contains(p);
    }

    /**
     * Si el competidor ya quedó afuera, devuelve la ronda en la que perdió por última vez.
     * Vacío si sigue en competencia, si no es de esta llave o si la llave terminó
     * (en ese caso conviene mirar el podio).
     * En el triangular, perder el primer combate no elimina: todavía pelea contra el más liviano.
     */
    public Optional<Ronda> rondaDeEliminacion(Participante p) {
        avanzar();
        if (campeon != null || !contiene(p)) {
            return Optional.empty();
        }
        for (int i = rondas.size() - 1; i >= 0; i--) {
            Ronda r = rondas.get(i);
            if (p.equals(r.getLibre())) {
                return Optional.empty();
            }
            for (Combate c : r.getCombates()) {
                if (c.participa(p)) {
                    return p.equals(c.getPerdedor()) ? Optional.of(r) : Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    // ----------------------------------------------------------------- corrección de resultados

    /**
     * Indica si se puede anular el resultado de ese combate: tiene que estar terminado
     * y ninguna ronda posterior puede tener resultados cargados.
     */
    public boolean puedeAnular(Combate combate) {
        avanzar();
        int indice = indiceDeRonda(combate);
        if (indice < 0 || !combate.estaTerminado()) {
            return false;
        }
        for (int i = indice + 1; i < rondas.size(); i++) {
            if (rondas.get(i).getCombates().stream().anyMatch(Combate::estaTerminado)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Anula el resultado de un combate para poder cargarlo de nuevo.
     * Las rondas que se habían armado a partir de ese resultado se descartan.
     *
     * @return el combate que quedó pendiente en su lugar (mismos competidores)
     */
    public Combate anularResultado(Combate combate) {
        if (!puedeAnular(combate)) {
            throw new IllegalStateException(indiceDeRonda(combate) < 0
                    ? "El combate no pertenece a esta llave"
                    : "No se puede anular: primero hay que anular los resultados de las rondas siguientes");
        }
        int indice = indiceDeRonda(combate);
        List<List<Combate>> anteriores = new ArrayList<>();
        for (int i = 0; i <= indice; i++) {
            anteriores.add(rondas.get(i).getCombates());
        }

        // Se vuelve a armar la llave desde cero y se repiten los resultados en el mismo orden.
        // El sorteo es determinístico, así que cada combate queda en la misma ronda y posición.
        // (Se busca por posición y no por competidores: en el triangular puede haber revancha.)
        rondas.clear();
        triangular = null;
        campeon = null;
        subcampeon = null;
        terceros.clear();
        avanzar();

        Combate reemplazo = null;
        for (int i = 0; i < anteriores.size(); i++) {
            List<Combate> viejos = anteriores.get(i);
            for (int j = 0; j < viejos.size(); j++) {
                Combate viejo = viejos.get(j);
                Combate nuevo = rondas.get(i).getCombates().get(j);
                if (!nuevo.getCompetidor1().equals(viejo.getCompetidor1())
                        || !nuevo.getCompetidor2().equals(viejo.getCompetidor2())) {
                    throw new IllegalStateException("No se pudo reconstruir la llave");
                }
                if (viejo == combate) {
                    reemplazo = nuevo;
                } else if (viejo.estaTerminado()) {
                    repetirResultado(viejo, nuevo);
                }
                avanzar();
            }
        }
        return reemplazo;
    }

    /**
     * Reemplaza el resultado de un combate terminado por el de {@code corregido}
     * (un combate aparte, con los mismos competidores, donde se cargó el resultado nuevo).
     * Así, si el usuario cancela la corrección, la llave no se toca.
     */
    public void corregirResultado(Combate original, Combate corregido) {
        if (!corregido.estaTerminado()
                || !corregido.getCompetidor1().equals(original.getCompetidor1())
                || !corregido.getCompetidor2().equals(original.getCompetidor2())) {
            throw new IllegalArgumentException("La corrección tiene que ser del mismo cruce y con resultado");
        }
        Combate reemplazo = anularResultado(original);
        repetirResultado(corregido, reemplazo);
        avanzar();
    }

    private int indiceDeRonda(Combate combate) {
        for (int i = 0; i < rondas.size(); i++) {
            if (rondas.get(i).getCombates().contains(combate)) {
                return i;
            }
        }
        return -1;
    }

    private static void repetirResultado(Combate viejo, Combate nuevo) {
        nuevo.registrar(viejo.getResultado());
    }

    // ----------------------------------------------------------------- generación de rondas

    /**
     * Genera rondas mientras la última esté terminada. Se llama desde las consultas,
     * así la llave avanza sola sin importar desde dónde se cargó el resultado.
     */
    private void avanzar() {
        while (campeon == null) {
            Ronda ultima = rondas.isEmpty() ? null : rondas.get(rondas.size() - 1);
            if (ultima != null && !ultima.estaTerminada()) {
                return;
            }
            if (triangular != null) {
                triangular.siguientePaso();
                continue;
            }
            List<Participante> enCarrera = ultima == null ? participantes : ultima.getClasificados();
            if (enCarrera.size() == 1) {
                cerrarEliminacion(enCarrera.get(0));
            } else if (enCarrera.size() == 3) {
                triangular = new Triangular(enCarrera);
            } else {
                rondas.add(rondaEliminatoria(enCarrera, ultima));
            }
        }
    }

    private Ronda rondaEliminatoria(List<Participante> enCarrera, Ronda anterior) {
        List<Participante> orden = new ArrayList<>(enCarrera);
        orden.sort(POR_PESO);

        Participante libre = null;
        if (orden.size() % 2 != 0) {
            Participante libreAnterior = anterior == null ? null : anterior.getLibre();
            libre = orden.stream()
                    .filter(p -> !p.equals(libreAnterior))
                    .findFirst()
                    .orElseThrow();
            orden.remove(libre);
        }

        List<Combate> combates = new ArrayList<>();
        for (int i = 0; i < orden.size(); i += 2) {
            combates.add(new Combate(orden.get(i), orden.get(i + 1)));
        }

        String nombre = switch (enCarrera.size()) {
            case 2 -> "Final";
            case 4 -> "Semifinal";
            default -> "Ronda " + (rondas.size() + 1);
        };
        return new Ronda(rondas.size() + 1, nombre, combates, libre);
    }

    private void cerrarEliminacion(Participante ganador) {
        campeon = ganador;
        if (rondas.isEmpty()) {
            return; // W.O.: un solo competidor
        }
        Ronda finalRonda = rondas.get(rondas.size() - 1);
        subcampeon = finalRonda.getPerdedores().get(0);
        if (rondas.size() >= 2) {
            // La ronda anterior a la final siempre es una semifinal de 4 sin pase libre
            terceros.addAll(rondas.get(rondas.size() - 2).getPerdedores());
        }
    }

    /**
     * Triangular de 3: A vs B, el perdedor contra C, y la final entre
     * el ganador de A-B y el ganador del segundo combate.
     */
    private final class Triangular {
        private final Participante c;
        private Combate combate1;
        private Combate combate2;
        private Combate finalTriangular;

        Triangular(List<Participante> tres) {
            List<Participante> orden = new ArrayList<>(tres);
            orden.sort(POR_PESO);
            c = orden.get(0); // el más liviano espera
            combate1 = new Combate(orden.get(1), orden.get(2));
            agregarRonda("Triangular 1", combate1, c);
        }

        void siguientePaso() {
            if (combate2 == null) {
                combate2 = new Combate(combate1.getPerdedor(), c);
                agregarRonda("Triangular 2", combate2, combate1.getGanador());
            } else if (finalTriangular == null) {
                finalTriangular = new Combate(combate1.getGanador(), combate2.getGanador());
                agregarRonda("Final", finalTriangular, null);
            } else {
                campeon = finalTriangular.getGanador();
                subcampeon = finalTriangular.getPerdedor();
                terceros.add(combate2.getPerdedor());
            }
        }

        private void agregarRonda(String nombre, Combate combate, Participante descansa) {
            rondas.add(new Ronda(rondas.size() + 1, nombre, List.of(combate), descansa));
        }
    }
}
