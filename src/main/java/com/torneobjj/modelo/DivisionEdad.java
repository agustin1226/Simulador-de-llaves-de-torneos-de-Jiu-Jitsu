package com.torneobjj.modelo;

import java.util.Optional;

/**
 * Divisiones de edad (solo mayores de 18).
 * La edad se calcula como la que el competidor cumple en el año del torneo.
 */
public enum DivisionEdad {
    ADULTO("Adulto", 18, 29),
    MASTER_1("Master 1", 30, 35),
    MASTER_2("Master 2", 36, 40),
    MASTER_3("Master 3", 41, 45),
    MASTER_4("Master 4", 46, 50),
    MASTER_5("Master 5", 51, 55),
    MASTER_6("Master 6", 56, 60),
    MASTER_7("Master 7", 61, Integer.MAX_VALUE);

    public static final int EDAD_MINIMA = 18;

    private final String nombre;
    private final int edadMinima;
    private final int edadMaxima;

    DivisionEdad(String nombre, int edadMinima, int edadMaxima) {
        this.nombre = nombre;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
    }

    /** Devuelve la división que corresponde a la edad, o vacío si es menor de 18. */
    public static Optional<DivisionEdad> desdeEdad(int edad) {
        for (DivisionEdad d : values()) {
            if (edad >= d.edadMinima && edad <= d.edadMaxima) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    public int getEdadMinima() {
        return edadMinima;
    }

    public int getEdadMaxima() {
        return edadMaxima;
    }

    /** Ej.: "Master 1 (30 a 35 años)" o "Master 7 (61+ años)". */
    public String getDescripcion() {
        return edadMaxima == Integer.MAX_VALUE
                ? nombre + " (" + edadMinima + "+ años)"
                : nombre + " (" + edadMinima + " a " + edadMaxima + " años)";
    }

    @Override
    public String toString() {
        return nombre;
    }
}
