package com.torneobjj.llave;

import static com.torneobjj.llave.Competidores.*;
import static org.junit.jupiter.api.Assertions.*;

import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Participante;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LlaveTest {

    private static void pelearHastaElFinal(Llave llave, Consumer<Combate> resultado) {
        int vueltas = 0;
        while (!llave.estaTerminada()) {
            llave.getCombatesPendientes().forEach(resultado);
            assertTrue(++vueltas < 100, "La llave no termina");
        }
    }

    private static List<String> pesos(Combate c) {
        return List.of(c.getCompetidor1().getApellido(), c.getCompetidor2().getApellido());
    }

    // ------------------------------------------------------------ validaciones

    @Test
    void sinCompetidoresNoHayLlave() {
        assertThrows(IllegalArgumentException.class, () -> new Llave(CATEGORIA, List.of()));
    }

    @Test
    void todosTienenQueEstarPesados() {
        List<Participante> lista = conPesos(80, 81);
        Participante sinPesar = new Participante("X", "Sin", "Pesar", java.time.LocalDate.of(2000, 1, 1),
                com.torneobjj.modelo.Sexo.MASCULINO, com.torneobjj.modelo.Nivel.AMATEUR,
                com.torneobjj.modelo.CategoriaPeso.MEDIO_PESADO);
        lista.add(sinPesar);
        assertThrows(IllegalArgumentException.class, () -> new Llave(CATEGORIA, lista));
    }

    @Test
    void sinRepetidos() {
        Participante p = conPeso(80);
        assertThrows(IllegalArgumentException.class, () -> new Llave(CATEGORIA, List.of(p, p)));
    }

    // ------------------------------------------------------------ 1 y 2 competidores

    @Test
    void unSoloCompetidorEsCampeonPorWO() {
        Llave llave = llave(80);
        assertTrue(llave.estaTerminada());
        assertTrue(llave.getRondas().isEmpty());
        assertEquals("80.0", llave.getCampeon().orElseThrow().getApellido());
        assertEquals(1, llave.getPodio().size());
    }

    @Test
    void dosCompetidoresVanAFinalDirecta() {
        Llave llave = llave(80, 85);
        assertEquals(1, llave.getRondas().size());
        assertEquals("Final", llave.getRondas().get(0).getNombre());
        assertFalse(llave.estaTerminada());
        assertTrue(llave.getPodio().isEmpty());

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasLiviano);

        assertTrue(llave.estaTerminada());
        List<Puesto> podio = llave.getPodio();
        assertEquals(new Puesto(1, conPeso(80)), podio.get(0));
        assertEquals(new Puesto(2, conPeso(85)), podio.get(1));
        assertEquals(2, podio.size());
    }

    // ------------------------------------------------------------ triangular de 3

    @Test
    void triangularLosDosMasPesadosEmpiezanYElLivianoEspera() {
        Llave llave = llave(85, 70, 80);
        Ronda r1 = llave.getRondas().get(0);
        assertEquals("Triangular 1", r1.getNombre());
        assertEquals(List.of("80.0", "85.0"), pesos(r1.getCombates().get(0)));
        assertEquals("70.0", r1.getLibre().getApellido());
    }

    @Test
    void triangularElPerdedorPeleaContraCYLaFinalEsContraElGanadorDeAB() {
        Llave llave = llave(70, 80, 85);

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado); // 85 le gana a 80
        Ronda r2 = llave.getRondas().get(1);
        assertEquals("Triangular 2", r2.getNombre());
        assertEquals(List.of("80.0", "70.0"), pesos(r2.getCombates().get(0)));
        assertEquals("85.0", r2.getLibre().getApellido());

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasLiviano); // 70 le gana a 80
        Ronda r3 = llave.getRondas().get(2);
        assertEquals("Final", r3.getNombre());
        assertEquals(List.of("85.0", "70.0"), pesos(r3.getCombates().get(0)));

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasLiviano); // 70 campeón
        assertEquals(List.of(new Puesto(1, conPeso(70)), new Puesto(2, conPeso(85)), new Puesto(3, conPeso(80))),
                llave.getPodio());
    }

    @Test
    void triangularPuedeHaberRevancha() {
        // A (80) pierde con B (85), le gana a C (70) y vuelve a pelear contra B en la final
        Llave llave = llave(70, 80, 85);
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado);
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado);
        assertEquals(List.of("85.0", "80.0"), pesos(llave.getCombatesPendientes().get(0)));
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasLiviano);
        assertEquals(List.of(new Puesto(1, conPeso(80)), new Puesto(2, conPeso(85)), new Puesto(3, conPeso(70))),
                llave.getPodio());
    }

    // ------------------------------------------------------------ eliminación

    @Test
    void cuatroCompetidoresSemifinalPorPesosParecidos() {
        Llave llave = llave(90, 70, 80, 75);
        Ronda semi = llave.getRondas().get(0);
        assertEquals("Semifinal", semi.getNombre());
        assertNull(semi.getLibre());
        assertEquals(List.of("70.0", "75.0"), pesos(semi.getCombates().get(0)));
        assertEquals(List.of("80.0", "90.0"), pesos(semi.getCombates().get(1)));

        pelearHastaElFinal(llave, Competidores::ganaElMasPesado);

        assertEquals(List.of(
                new Puesto(1, conPeso(90)), new Puesto(2, conPeso(75)),
                new Puesto(3, conPeso(70)), new Puesto(3, conPeso(80))), llave.getPodio());
    }

    @Test
    void conImparesPasaLibreElMasLiviano() {
        Llave llave = llave(78, 82, 70, 88, 85);
        Ronda r1 = llave.getRondas().get(0);
        assertEquals("70.0", r1.getLibre().getApellido());
        assertEquals(List.of("78.0", "82.0"), pesos(r1.getCombates().get(0)));
        assertEquals(List.of("85.0", "88.0"), pesos(r1.getCombates().get(1)));
    }

    @Test
    void cincoCompetidoresTerminanEnTriangular() {
        Llave llave = llave(70, 78, 82, 85, 88);
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado);
        // Quedan 82, 88 y 70 (libre) → triangular: 82 vs 88, 70 espera
        Ronda r2 = llave.getRondas().get(1);
        assertEquals("Triangular 1", r2.getNombre());
        assertEquals(List.of("82.0", "88.0"), pesos(r2.getCombates().get(0)));
        assertEquals("70.0", r2.getLibre().getApellido());
    }

    @Test
    void sieteCompetidoresLibreYDespuesSemifinal() {
        Llave llave = llave(70, 72, 74, 76, 78, 80, 82);
        Ronda r1 = llave.getRondas().get(0);
        assertEquals("Ronda 1", r1.getNombre());
        assertEquals("70.0", r1.getLibre().getApellido());
        assertEquals(3, r1.getCombates().size());

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado);
        Ronda semi = llave.getRondas().get(1);
        assertEquals("Semifinal", semi.getNombre());
        assertEquals(List.of("70.0", "74.0"), pesos(semi.getCombates().get(0)));
        assertEquals(List.of("78.0", "82.0"), pesos(semi.getCombates().get(1)));
    }

    @Test
    void nadiePasaLibreDosRondasSeguidas() {
        // 9 → pasa libre 60; ganan los livianos → quedan 60 (libre), 70, 74, 78, 82
        // 5 en ronda 2: 60 no puede repetir, pasa libre el siguiente más liviano (70)
        Llave llave = llave(60, 70, 72, 74, 76, 78, 80, 82, 84);
        assertEquals("60.0", llave.getRondas().get(0).getLibre().getApellido());

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasLiviano);
        Ronda r2 = llave.getRondas().get(1);
        assertEquals("70.0", r2.getLibre().getApellido());
        assertEquals(List.of("60.0", "74.0"), pesos(r2.getCombates().get(0)));
        assertEquals(List.of("78.0", "82.0"), pesos(r2.getCombates().get(1)));
    }

    @Test
    void laSiguienteRondaNoApareceHastaTerminarLaActual() {
        Llave llave = llave(70, 75, 80, 85);
        List<Combate> pendientes = llave.getCombatesPendientes();
        Competidores.ganaElMasPesado(pendientes.get(0));
        assertEquals(1, llave.getRondas().size());
        assertEquals(1, llave.getCombatesPendientes().size());
        Competidores.ganaElMasPesado(pendientes.get(1));
        assertEquals(2, llave.getRondas().size());
    }

    // ------------------------------------------------------------ propiedades generales

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 23, 32, 33})
    void cualquierCantidadTerminaConUnPodioValido(int n) {
        for (long semilla = 0; semilla < 20; semilla++) {
            Random random = new Random(semilla);
            double[] pesosAlAzar = new double[n];
            for (int i = 0; i < n; i++) {
                pesosAlAzar[i] = 70 + i * 0.5 + random.nextInt(3) * 0.1;
            }
            Llave llave = llave(pesosAlAzar);
            pelearHastaElFinal(llave, c -> Competidores.ganaAlAzar(c, random));

            Participante libreAnterior = null;
            for (Ronda r : llave.getRondas()) {
                Set<Participante> enLaRonda = new HashSet<>();
                for (Combate c : r.getCombates()) {
                    assertTrue(enLaRonda.add(c.getCompetidor1()));
                    assertTrue(enLaRonda.add(c.getCompetidor2()));
                }
                if (r.getLibre() != null) {
                    assertTrue(enLaRonda.add(r.getLibre()), "El libre no puede pelear en su ronda");
                    if (!r.getNombre().startsWith("Triangular")) {
                        assertNotEquals(libreAnterior, r.getLibre(), "Pase libre repetido");
                    }
                }
                libreAnterior = r.getLibre();
            }

            List<Puesto> podio = llave.getPodio();
            assertEquals(1, podio.get(0).lugar());
            Set<Participante> enPodio = new HashSet<>();
            podio.forEach(p -> assertTrue(enPodio.add(p.participante())));
            // Si terminó en triangular hay un solo tercero; si terminó en semifinal + final, dos
            boolean terminoEnTriangular = llave.getRondas().stream()
                    .anyMatch(r -> r.getNombre().startsWith("Triangular"));
            int esperado = n == 1 ? 1 : n == 2 ? 2 : terminoEnTriangular ? 3 : 4;
            assertEquals(esperado, podio.size(), "Podio para n=" + n);
        }
    }

    @Test
    void rondaDeEliminacion() {
        Llave llave = llave(70, 75, 80, 85, 90);
        Participante p75 = llave.getParticipantes().get(1);
        Participante p70 = llave.getParticipantes().get(0);
        assertTrue(llave.rondaDeEliminacion(p75).isEmpty(), "Todavía no peleó");

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado); // pierden 75 y 85
        assertEquals("Ronda 1", llave.rondaDeEliminacion(p75).orElseThrow().getNombre());
        assertTrue(llave.rondaDeEliminacion(p70).isEmpty(), "Pasó libre");

        // Quedan 70, 80 y 90 → triangular: 80 vs 90, 70 espera
        llave.getCombatesPendientes().forEach(Competidores::ganaElMasPesado); // pierde 80
        Participante p80 = llave.getParticipantes().get(2);
        assertTrue(llave.rondaDeEliminacion(p80).isEmpty(), "En el triangular perder el primero no elimina");

        llave.getCombatesPendientes().forEach(Competidores::ganaElMasLiviano); // 70 le gana a 80
        assertEquals("Triangular 2", llave.rondaDeEliminacion(p80).orElseThrow().getNombre());
    }

    @Test
    void elOrdenDeInscripcionNoCambiaElSorteo() {
        List<Participante> a = conPesos(70, 75, 80, 85, 90, 95, 100);
        List<Participante> b = new ArrayList<>(a);
        java.util.Collections.reverse(b);
        assertEquals(new Llave(CATEGORIA, a).getRondas().toString(),
                new Llave(CATEGORIA, b).getRondas().toString());
    }
}
