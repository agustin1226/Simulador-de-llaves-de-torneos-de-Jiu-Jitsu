package com.torneobjj.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CategoriaPesoTest {

    @Test
    void hombresTienenLasNueveCategorias() {
        assertEquals(List.of(CategoriaPeso.values()), CategoriaPeso.disponiblesPara(Sexo.MASCULINO));
    }

    @Test
    void mujeresNoTienenPesadisimo() {
        List<CategoriaPeso> fem = CategoriaPeso.disponiblesPara(Sexo.FEMENINO);
        assertEquals(8, fem.size());
        assertFalse(fem.contains(CategoriaPeso.PESADISIMO));
        assertThrows(IllegalArgumentException.class, () -> CategoriaPeso.PESADISIMO.getLimite(Sexo.FEMENINO));
    }

    @Test
    void limitesSegunTablaIbjjf() {
        assertEquals(88.3, CategoriaPeso.MEDIO_PESADO.getLimite(Sexo.MASCULINO));
        assertEquals(74.0, CategoriaPeso.MEDIO_PESADO.getLimite(Sexo.FEMENINO));
        assertEquals(100.5, CategoriaPeso.SUPER_PESADO.getLimite(Sexo.MASCULINO));
        assertEquals(48.5, CategoriaPeso.GALLO.getLimite(Sexo.FEMENINO));
    }

    @Test
    void elLimiteEsInclusive() {
        assertTrue(CategoriaPeso.MEDIO_PESADO.admitePeso(Sexo.MASCULINO, 88.3));
        assertFalse(CategoriaPeso.MEDIO_PESADO.admitePeso(Sexo.MASCULINO, 88.4));
    }

    @Test
    void categoriasSinLimite() {
        assertFalse(CategoriaPeso.PESADISIMO.tieneLimite(Sexo.MASCULINO));
        assertFalse(CategoriaPeso.SUPER_PESADO.tieneLimite(Sexo.FEMENINO));
        assertTrue(CategoriaPeso.SUPER_PESADO.admitePeso(Sexo.FEMENINO, 140));
        assertTrue(CategoriaPeso.SUPER_PESADO.tieneLimite(Sexo.MASCULINO));
    }

    @Test
    void descripcionParaElCombo() {
        assertEquals("Medio pesado (hasta 88,3 kg)", CategoriaPeso.MEDIO_PESADO.getDescripcion(Sexo.MASCULINO));
        assertEquals("Súper pesado (sin límite)", CategoriaPeso.SUPER_PESADO.getDescripcion(Sexo.FEMENINO));
    }
}
