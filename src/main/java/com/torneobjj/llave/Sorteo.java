package com.torneobjj.llave;

import com.torneobjj.modelo.Categoria;
import com.torneobjj.modelo.Participante;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Agrupa a los competidores por categoría y arma una llave para cada una. */
public final class Sorteo {

    private Sorteo() {
    }

    /**
     * Arma las llaves del torneo. Solo entran los que pasaron por el pesaje;
     * los que no se pesaron quedan afuera (ausentes).
     *
     * @return llaves ordenadas por categoría
     */
    public static Map<Categoria, Llave> sortear(List<Participante> inscriptos, int anioTorneo) {
        Map<Categoria, List<Participante>> grupos = new TreeMap<>();
        for (Participante p : inscriptos) {
            if (p.estaPesado()) {
                grupos.computeIfAbsent(p.getCategoria(anioTorneo), k -> new ArrayList<>()).add(p);
            }
        }
        Map<Categoria, Llave> llaves = new LinkedHashMap<>();
        grupos.forEach((categoria, competidores) -> llaves.put(categoria, new Llave(categoria, competidores)));
        return llaves;
    }
}
