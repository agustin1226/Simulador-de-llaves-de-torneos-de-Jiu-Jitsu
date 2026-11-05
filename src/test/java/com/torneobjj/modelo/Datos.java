package com.torneobjj.modelo;

import java.time.LocalDate;

/** Ayudas para crear participantes en los tests. */
final class Datos {

    private static int contador = 0;

    private Datos() {
    }

    static Participante participante(Sexo sexo, CategoriaPeso peso) {
        contador++;
        return new Participante("DNI" + contador, "Nombre" + contador, "Apellido" + contador,
                LocalDate.of(1995, 5, 10), sexo, Nivel.AMATEUR, peso);
    }

    static Participante hombre() {
        return participante(Sexo.MASCULINO, CategoriaPeso.MEDIO_PESADO);
    }
}
