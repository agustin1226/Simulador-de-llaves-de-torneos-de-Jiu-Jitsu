package com.torneobjj.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DivisionEdadTest {

    @ParameterizedTest
    @CsvSource({
            "18, ADULTO", "29, ADULTO",
            "30, MASTER_1", "35, MASTER_1",
            "36, MASTER_2", "40, MASTER_2",
            "41, MASTER_3", "45, MASTER_3",
            "46, MASTER_4", "50, MASTER_4",
            "51, MASTER_5", "55, MASTER_5",
            "56, MASTER_6", "60, MASTER_6",
            "61, MASTER_7", "90, MASTER_7"
    })
    void cadaEdadCaeEnUnaSolaDivision(int edad, DivisionEdad esperada) {
        assertEquals(Optional.of(esperada), DivisionEdad.desdeEdad(edad));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 16, 17})
    void menoresDe18NoTienenDivision(int edad) {
        assertTrue(DivisionEdad.desdeEdad(edad).isEmpty());
    }

    @org.junit.jupiter.api.Test
    void descripciones() {
        assertEquals("Master 1 (30 a 35 años)", DivisionEdad.MASTER_1.getDescripcion());
        assertEquals("Master 7 (61+ años)", DivisionEdad.MASTER_7.getDescripcion());
    }
}
