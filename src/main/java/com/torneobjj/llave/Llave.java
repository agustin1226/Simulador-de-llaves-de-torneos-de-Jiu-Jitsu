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
