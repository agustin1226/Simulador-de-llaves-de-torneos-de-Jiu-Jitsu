package com.torneobjj.modelo;

public enum Nivel {
    PRINCIPIANTE("Principiante"),
    AMATEUR("Amateur"),
    PROFESIONAL("Profesional");

    private final String nombre;

    Nivel(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
