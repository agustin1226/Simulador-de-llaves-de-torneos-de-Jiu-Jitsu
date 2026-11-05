package com.torneobjj.modelo;

/** Puntaje de un competidor al terminar un combate. */
public record Marcador(int puntos, int ventajas, int penalidades) {

    public Marcador {
        if (puntos < 0 || ventajas < 0 || penalidades < 0) {
            throw new IllegalArgumentException("El marcador no puede tener valores negativos");
        }
    }

    public static Marcador soloPuntos(int puntos) {
        return new Marcador(puntos, 0, 0);
    }
}
