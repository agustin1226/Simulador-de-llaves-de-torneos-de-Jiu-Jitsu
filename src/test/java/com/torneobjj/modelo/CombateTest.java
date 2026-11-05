package com.torneobjj.modelo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CombateTest {

    private Participante a;
    private Participante b;
    private Combate combate;

    @BeforeEach
    void preparar() {
        a = Datos.hombre();
        b = Datos.hombre();
        combate = new Combate(a, b);
    }

    @Test
    void sumision() {
        combate.registrarSumision(b);
        assertTrue(combate.estaTerminado());
        assertEquals(b, combate.getGanador());
        assertEquals(a, combate.getPerdedor());
        assertEquals(TipoVictoria.SUMISION, combate.getResultado().tipo());
    }

    @Test
    void ganaElDeMasPuntos() {
        combate.registrarPorPuntos(Marcador.soloPuntos(2), Marcador.soloPuntos(4));
        assertEquals(b, combate.getGanador());
        assertEquals(TipoVictoria.PUNTOS, combate.getResultado().tipo());
    }

    @Test
    void conPuntosIgualesDecidenLasVentajas() {
        combate.registrarPorPuntos(new Marcador(2, 3, 0), new Marcador(2, 1, 0));
        assertEquals(a, combate.getGanador());
        assertEquals(TipoVictoria.VENTAJAS, combate.getResultado().tipo());
    }

    @Test
    void despuesDecidenLasPenalidades() {
        combate.registrarPorPuntos(new Marcador(2, 1, 2), new Marcador(2, 1, 1));
        assertEquals(b, combate.getGanador());
        assertEquals(TipoVictoria.PENALIDADES, combate.getResultado().tipo());
    }

    @Test
    void enEmpateTotalPierdeElExcedidoDePeso() {
        a.registrarPeso(90.0); // límite 88,3
        b.registrarPeso(85.0);
        combate.registrarPorPuntos(Marcador.soloPuntos(0), Marcador.soloPuntos(0));
        assertEquals(b, combate.getGanador());
        assertEquals(TipoVictoria.PESO, combate.getResultado().tipo());
    }

    @Test
    void elExcedidoPuedeGanarSiNoHayEmpate() {
        a.registrarPeso(90.0);
        b.registrarPeso(85.0);
        combate.registrarPorPuntos(Marcador.soloPuntos(2), Marcador.soloPuntos(0));
        assertEquals(a, combate.getGanador());
    }

    @Test
    void empateTotalSinExcedidosLoDecideElArbitro() {
        Marcador cero = Marcador.soloPuntos(0);
        assertTrue(combate.requiereDecisionArbitro(cero, cero));
        assertThrows(IllegalStateException.class, () -> combate.registrarPorPuntos(cero, cero));

        combate.registrarDecisionArbitro(cero, cero, a);
        assertEquals(a, combate.getGanador());
        assertEquals(TipoVictoria.DECISION_ARBITRO, combate.getResultado().tipo());
    }

    @Test
    void empateTotalConAmbosExcedidosLoDecideElArbitro() {
        a.registrarPeso(90.0);
        b.registrarPeso(91.0);
        Marcador m = new Marcador(4, 1, 1);
        assertTrue(combate.requiereDecisionArbitro(m, m));
    }

    @Test
    void noHayDecisionDelArbitroSiElMarcadorDefine() {
        assertThrows(IllegalStateException.class, () ->
                combate.registrarDecisionArbitro(Marcador.soloPuntos(2), Marcador.soloPuntos(0), b));
    }

    @Test
    void noSePuedeCargarDosVeces() {
        combate.registrarSumision(a);
        assertThrows(IllegalStateException.class, () -> combate.registrarSumision(b));
    }

    @Test
    void elGanadorTieneQueSerDelCombate() {
        Participante otro = Datos.hombre();
        assertThrows(IllegalArgumentException.class, () -> combate.registrarSumision(otro));
    }

    @Test
    void noPuedePelearContraSiMismo() {
        assertThrows(IllegalArgumentException.class, () -> new Combate(a, a));
    }

    @Test
    void marcadorNoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Marcador(-1, 0, 0));
    }
}
