package com.torneobjj.modelo;

public enum TipoVictoria {
    SUMISION("Sumisión"),
    PUNTOS("Puntos"),
    VENTAJAS("Ventajas"),
    PENALIDADES("Penalidades"),
    /** Empate total: pierde el que se pasó del peso de su categoría. */
    PESO("Empate - rival excedido de peso"),
    DECISION_ARBITRO("Decisión del árbitro");

    private final String nombre;

    TipoVictoria(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
