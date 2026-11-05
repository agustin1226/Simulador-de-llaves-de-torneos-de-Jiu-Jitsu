package com.torneobjj.llave;

import static com.torneobjj.llave.Competidores.*;
import static org.junit.jupiter.api.Assertions.*;

import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Marcador;
import com.torneobjj.modelo.TipoVictoria;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AnularResultadoTest {

    @Test
    void anularEnLaRondaActualDejaElCombatePendiente() {
        Llave llave = llave(70, 75, 80, 85);
        Combate primero = llave.getCombatesPendientes().get(0);
        Combate segundo = llave.getCombatesPendientes().get(1);
        primero.registrarSumision(primero.getCompetidor1());

        Combate reemplazo = llave.anularResultado(primero);

        assertFalse(reemplazo.estaTerminado());
        assertEquals(primero.getCompetidor1(), reemplazo.getCompetidor1());
        assertEquals(primero.getCompetidor2(), reemplazo.getCompetidor2());
        assertEquals(2, llave.getCombatesPendientes().size());
        assertTrue(llave.getCombatesPendientes().contains(reemplazo));
        assertFalse(llave.getCombatesPendientes().contains(segundo), "Los combates se reconstruyen");
    }

    @Test
    void anularElUltimoCombateDeUnaRondaBorraLaRondaSiguiente() {
        Llave llave = llave(70, 75, 80, 85);
        List<Combate> semis = llave.getCombatesPendientes();
        semis.get(0).registrarPorPuntos(new Marcador(4, 0, 0), new Marcador(2, 0, 0));
        semis.get(1).registrarSumision(semis.get(1).getCompetidor2());
        assertEquals(2, llave.getRondas().size(), "Ya se armó la final");

        Combate reemplazo = llave.anularResultado(semis.get(1));

        assertEquals(1, llave.getRondas().size(), "La final se descarta");
        assertEquals(List.of(reemplazo), llave.getCombatesPendientes());
        Combate semi1 = llave.getRondas().get(0).getCombates().get(0);
        assertEquals(TipoVictoria.PUNTOS, semi1.getResultado().tipo(), "Se conserva el otro resultado");
        assertEquals(new Marcador(4, 0, 0), semi1.getResultado().marcador1());

        reemplazo.registrarSumision(reemplazo.getCompetidor1()); // ahora gana el otro
        assertEquals(List.of("70.0", "80.0"),
                List.of(llave.getCombatesPendientes().get(0).getCompetidor1().getApellido(),
                        llave.getCombatesPendientes().get(0).getCompetidor2().getApellido()));
    }

    @Test
    void noSePuedeAnularSiHayResultadosEnRondasPosteriores() {
        Llave llave = llave(70, 75, 80, 85);
        List<Combate> semis = llave.getCombatesPendientes();
        semis.forEach(Competidores::ganaElMasPesado);
        Combate finalCombate = llave.getCombatesPendientes().get(0);
        finalCombate.registrarSumision(finalCombate.getCompetidor1());

        assertFalse(llave.puedeAnular(semis.get(0)));
        assertThrows(IllegalStateException.class, () -> llave.anularResultado(semis.get(0)));
        assertTrue(llave.puedeAnular(finalCombate));
    }

    @Test
    void anularLaFinalReabreLaLlave() {
        Llave llave = llave(70, 75);
        Combate fin = llave.getCombatesPendientes().get(0);
        fin.registrarSumision(fin.getCompetidor1());
        assertTrue(llave.estaTerminada());

        llave.anularResultado(fin);

        assertFalse(llave.estaTerminada());
        assertTrue(llave.getCampeon().isEmpty());
        assertTrue(llave.getPodio().isEmpty());
    }

    @Test
    void anularEnElTriangular() {
        Llave llave = llave(70, 80, 85);
        Combate c1 = llave.getCombatesPendientes().get(0);
        c1.registrarSumision(c1.getCompetidor2()); // gana 85
        Combate c2 = llave.getCombatesPendientes().get(0);
        c2.registrarSumision(c2.getCompetidor2()); // gana 70
        assertEquals("Final", llave.getRondas().get(2).getNombre());

        // Se corrige el primer combate del triangular: ahora gana 80
        llave.anularResultado(c2);
        Combate nuevoC1 = llave.anularResultado(llave.getRondas().get(0).getCombates().get(0));
        nuevoC1.registrarSumision(nuevoC1.getCompetidor1());

        Ronda r2 = llave.getRondas().get(1);
        assertEquals("Triangular 2", r2.getNombre());
        assertEquals("85.0", r2.getCombates().get(0).getCompetidor1().getApellido());
        assertEquals("80.0", r2.getLibre().getApellido());
    }

    @Test
    void anularLaFinalDeUnTriangularConRevancha() {
        // 85 le gana a 80, 80 le gana a 70, y la final es otra vez 85 vs 80
        Llave llave = llave(70, 80, 85);
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado);
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado);
        Combate fin = llave.getCombatesPendientes().get(0);
        fin.registrarSumision(fin.getCompetidor2()); // gana 80

        Combate nuevo = llave.anularResultado(fin);

        assertEquals("Final", llave.getRondas().get(2).getNombre());
        assertSame(nuevo, llave.getRondas().get(2).getCombates().get(0));
        assertTrue(llave.getRondas().get(0).getCombates().get(0).estaTerminado(), "El primer cruce conserva su resultado");
        assertFalse(nuevo.estaTerminado());
    }

    @Test
    void corregirResultadoCambiaElGanadorYRearmaLaRondaSiguiente() {
        Llave llave = llave(70, 75, 80, 85);
        List<Combate> semis = llave.getCombatesPendientes();
        semis.forEach(Competidores::ganaElMasPesado); // final: 75 vs 85

        Combate corregido = new Combate(semis.get(0).getCompetidor1(), semis.get(0).getCompetidor2());
        corregido.registrarPorPuntos(new Marcador(6, 0, 0), new Marcador(0, 0, 0)); // gana 70
        llave.corregirResultado(semis.get(0), corregido);

        Combate semi1 = llave.getRondas().get(0).getCombates().get(0);
        assertEquals("70.0", semi1.getGanador().getApellido());
        assertEquals(TipoVictoria.PUNTOS, semi1.getResultado().tipo());
        Combate fin = llave.getCombatesPendientes().get(0);
        assertEquals(List.of("70.0", "85.0"), List.of(fin.getCompetidor1().getApellido(), fin.getCompetidor2().getApellido()));
    }

    @Test
    void corregirExigeElMismoCruceConResultado() {
        Llave llave = llave(70, 75);
        Combate fin = llave.getCombatesPendientes().get(0);
        fin.registrarSumision(fin.getCompetidor1());
        Combate sinResultado = new Combate(fin.getCompetidor1(), fin.getCompetidor2());
        assertThrows(IllegalArgumentException.class, () -> llave.corregirResultado(fin, sinResultado));
        Combate otroCruce = new Combate(fin.getCompetidor2(), fin.getCompetidor1());
        otroCruce.registrarSumision(fin.getCompetidor1());
        assertThrows(IllegalArgumentException.class, () -> llave.corregirResultado(fin, otroCruce));
        assertTrue(llave.estaTerminada(), "Un error no modifica la llave");
    }

    @Test
    void noSePuedeAnularUnCombatePendienteNiAjeno() {
        Llave llave = llave(70, 75);
        Combate pendiente = llave.getCombatesPendientes().get(0);
        assertFalse(llave.puedeAnular(pendiente));
        Combate ajeno = new Combate(conPeso(60), conPeso(61));
        ajeno.registrarSumision(ajeno.getCompetidor1());
        assertFalse(llave.puedeAnular(ajeno));
        assertThrows(IllegalStateException.class, () -> llave.anularResultado(ajeno));
    }

    @Test
    void anularYVolverACargarDaLaMismaLlave() {
        for (int n = 4; n <= 17; n++) {
            Random random = new Random(n);
            double[] pesos = new double[n];
            for (int i = 0; i < n; i++) {
                pesos[i] = 70 + i;
            }
            Llave llave = llave(pesos);
            while (!llave.estaTerminada()) {
                for (Combate c : llave.getCombatesPendientes()) {
                    Competidores.ganaAlAzar(c, random);
                }
            }
            String antes = llave.getRondas().toString();
            List<Puesto> podioAntes = llave.getPodio();

            // Se anula la final y se vuelve a cargar con el mismo ganador
            Combate fin = llave.getRondas().get(llave.getRondas().size() - 1).getCombates().get(0);
            var ganador = fin.getGanador();
            Combate nuevo = llave.anularResultado(fin);
            nuevo.registrarSumision(ganador);

            assertEquals(podioAntes, llave.getPodio(), "n=" + n);
            assertEquals(antes.replaceAll("@\\w+", ""), llave.getRondas().toString().replaceAll("@\\w+", ""));
        }
    }
}
