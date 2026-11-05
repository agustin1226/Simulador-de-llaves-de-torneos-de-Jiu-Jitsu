package com.torneobjj.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ParticipanteTest {

    private Participante nacidoEl(LocalDate fecha) {
        return new Participante("123", "Juan", "Pérez", fecha,
                Sexo.MASCULINO, Nivel.PRINCIPIANTE, CategoriaPeso.MEDIO_PESADO);
    }

    @Test
    void laEdadEsLaQueCumpleEnElAnioDelTorneo() {
        // Cumple 30 en diciembre de 2026: para todo el torneo 2026 ya es Master 1
        Participante p = nacidoEl(LocalDate.of(1996, 12, 31));
        assertEquals(30, p.getEdadEnAnio(2026));
        assertEquals(DivisionEdad.MASTER_1, p.getCategoria(2026).division());
    }

    @Test
    void menorDe18NoTieneCategoria() {
        Participante p = nacidoEl(LocalDate.of(2009, 1, 1)); // cumple 17 en 2026
        assertThrows(IllegalStateException.class, () -> p.getCategoria(2026));
    }

    @Test
    void categoriaCompleta() {
        Participante p = nacidoEl(LocalDate.of(2000, 3, 15));
        assertEquals(new Categoria(Sexo.MASCULINO, DivisionEdad.ADULTO, CategoriaPeso.MEDIO_PESADO, Nivel.PRINCIPIANTE),
                p.getCategoria(2026));
    }

    @Test
    void noSePuedeInscribirEnUnaCategoriaQueNoExisteParaSuSexo() {
        assertThrows(IllegalArgumentException.class, () -> new Participante("1", "Ana", "Gómez",
                LocalDate.of(1990, 1, 1), Sexo.FEMENINO, Nivel.AMATEUR, CategoriaPeso.PESADISIMO));
    }

    @Test
    void camposObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> new Participante(" ", "Ana", "Gómez",
                LocalDate.of(1990, 1, 1), Sexo.FEMENINO, Nivel.AMATEUR, CategoriaPeso.GALLO));
        assertThrows(NullPointerException.class, () -> new Participante("1", "Ana", "Gómez",
                LocalDate.of(1990, 1, 1), Sexo.FEMENINO, null, CategoriaPeso.GALLO));
    }

    @Test
    void pesaje() {
        Participante p = nacidoEl(LocalDate.of(1990, 1, 1));
        assertFalse(p.estaPesado());
        assertFalse(p.isExcedidoDePeso());

        p.registrarPeso(88.3);
        assertTrue(p.estaPesado());
        assertFalse(p.isExcedidoDePeso());

        p.registrarPeso(89.0); // se puede corregir el pesaje
        assertTrue(p.isExcedidoDePeso());
    }

    @Test
    void pesoFueraDeRango() {
        Participante p = nacidoEl(LocalDate.of(1990, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> p.registrarPeso(0));
        assertThrows(IllegalArgumentException.class, () -> p.registrarPeso(500));
    }

    @Test
    void igualdadPorDni() {
        Participante a = nacidoEl(LocalDate.of(1990, 1, 1));
        Participante b = nacidoEl(LocalDate.of(1980, 1, 1));
        assertEquals(a, b);
    }
}
