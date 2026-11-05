package com.torneobjj.modelo;

import java.util.Objects;

/**
 * Combinación que define contra quién pelea cada competidor.
 * Al ser un record tiene equals/hashCode, así que sirve como clave de un Map.
 */
public record Categoria(Sexo sexo, DivisionEdad division, CategoriaPeso peso, Nivel nivel) {

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
    public String toString() {
        return getNombre();
    }
}
