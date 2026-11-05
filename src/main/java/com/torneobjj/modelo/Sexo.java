package com.torneobjj.modelo;

public enum Sexo {
    MASCULINO("Masculino"),
    FEMENINO("Femenino");

    private final String nombre;

    Sexo(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
