package com.torneobjj.llave;

import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Participante;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una ronda de la llave: sus combates y, si corresponde, el competidor
 * que descansa (pase libre, o el que espera en el triangular de 3).
 */
public class Ronda {

    private final int numero;
    private final String nombre;
    private final List<Combate> combates;
    private final Participante libre;

    Ronda(int numero, String nombre, List<Combate> combates, Participante libre) {
        this.numero = numero;
        this.nombre = nombre;
        this.combates = List.copyOf(combates);
        this.libre = libre;
    }

    public boolean estaTerminada() {
        return combates.stream().allMatch(Combate::estaTerminado);
    }

    /** Ganadores de los combates más el que descansó. Solo tiene sentido con la ronda terminada. */
    List<Participante> getClasificados() {
        List<Participante> lista = new ArrayList<>();
        for (Combate c : combates) {
            lista.add(c.getGanador());
        }
        if (libre != null) {
            lista.add(libre);
        }
        return lista;
    }

    List<Participante> getPerdedores() {
        List<Participante> lista = new ArrayList<>();
        for (Combate c : combates) {
            lista.add(c.getPerdedor());
        }
        return lista;
    }

    public int getNumero() { return numero; }
    /** Ej.: "Ronda 1", "Semifinal", "Final", "Triangular 1". */
    public String getNombre() { return nombre; }
    public List<Combate> getCombates() { return Collections.unmodifiableList(combates); }
    /** El que descansa esta ronda, o null. */
    public Participante getLibre() { return libre; }

    @Override
    public String toString() {
        return nombre + " " + combates + (libre != null ? " (libre: " + libre + ")" : "");
    }
}
