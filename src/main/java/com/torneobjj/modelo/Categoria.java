package com.torneobjj.modelo;

import java.util.Comparator;
import java.util.Objects;

/**
 * Combinación que define contra quién pelea cada competidor.
 * Al ser un record tiene equals/hashCode, así que sirve como clave de un Map.
 * Se ordena por sexo, división, peso y nivel (para listarlas en la interfaz).
 */
public record Categoria(Sexo sexo, DivisionEdad division, CategoriaPeso peso, Nivel nivel)
        implements Comparable<Categoria> {

    private static final Comparator<Categoria> ORDEN = Comparator
            .comparing(Categoria::sexo)
            .thenComparing(Categoria::division)
            .thenComparing(Categoria::peso)
            .thenComparing(Categoria::nivel);

    public Categoria {
        Objects.requireNonNull(sexo);
        Objects.requireNonNull(division);
        Objects.requireNonNull(peso);
        Objects.requireNonNull(nivel);
    }

    /** Ej.: "Masculino / Master 1 / Medio pesado / Amateur". */
    public String getNombre() {
        return sexo + " / " + division + " / " + peso + " / " + nivel;
    }

    @Override
    public int compareTo(Categoria otra) {
        return ORDEN.compare(this, otra);
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
