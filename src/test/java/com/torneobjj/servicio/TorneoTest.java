package com.torneobjj.servicio;

import static org.junit.jupiter.api.Assertions.*;

import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.Nivel;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Sexo;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TorneoTest {

    private Torneo torneo;

    @BeforeEach
    void preparar() {
        torneo = new Torneo("Open de prueba", LocalDate.of(2026, 11, 20));
    }

    private static Participante competidor(String dni, String nombre, String apellido, int anioNacimiento) {
        return new Participante(dni, nombre, apellido, LocalDate.of(anioNacimiento, 3, 3),
                Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO);
    }

    @Test
    void inscribirValidaDniRepetido() {
        torneo.inscribir(competidor("1", "Juan", "Pérez", 2000));
        assertThrows(IllegalArgumentException.class, () -> torneo.inscribir(competidor("1", "Otro", "X", 1999)));
    }

    @Test
    void inscribirValidaEdadMinima() {
        assertThrows(IllegalArgumentException.class, () -> torneo.inscribir(competidor("1", "Joven", "X", 2009)));
        torneo.inscribir(competidor("2", "Justo", "X", 2008)); // cumple 18 en 2026
        assertEquals(1, torneo.getParticipantes().size());
    }

    @Test
    void inscribirValidaFechaFutura() {
        Participante p = new Participante("1", "A", "B", LocalDate.of(2026, 12, 1),
                Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.GALLO);
        assertThrows(IllegalArgumentException.class, () -> torneo.inscribir(p));
    }

    @Test
    void sorteoSoloConPesados() {
        Participante a = competidor("1", "A", "A", 2000);
        Participante b = competidor("2", "B", "B", 2000);
        torneo.inscribir(a);
        torneo.inscribir(b);
        assertThrows(IllegalStateException.class, torneo::sortear);

        torneo.registrarPeso(a, 85);
        torneo.sortear();
        assertTrue(torneo.estaSorteado());
        assertEquals(List.of(b), torneo.getAusentes());
        assertTrue(torneo.llaveDe(a).isPresent());
        assertTrue(torneo.llaveDe(b).isEmpty());
    }

    @Test
    void despuesDelSorteoNoSeInscribeNiSePesa() {
        Participante a = competidor("1", "A", "A", 2000);
        torneo.inscribir(a);
        torneo.registrarPeso(a, 85);
        torneo.sortear();
        assertThrows(IllegalStateException.class, () -> torneo.inscribir(competidor("2", "B", "B", 2000)));
        assertThrows(IllegalStateException.class, () -> torneo.registrarPeso(a, 86));
        assertThrows(IllegalStateException.class, () -> torneo.eliminar(a));
        assertThrows(IllegalStateException.class, torneo::sortear);
    }

    @Test
    void anularSorteoSoloSinResultados() {
        Participante a = competidor("1", "A", "A", 2000);
        Participante b = competidor("2", "B", "B", 2000);
        torneo.inscribir(a);
        torneo.inscribir(b);
        torneo.registrarPeso(a, 85);
        torneo.registrarPeso(b, 86);
        torneo.sortear();

        torneo.anularSorteo();
        assertFalse(torneo.estaSorteado());
        assertTrue(torneo.getLlaves().isEmpty());

        torneo.sortear();
        torneo.llaveDe(a).orElseThrow().getCombatesPendientes().get(0).registrarSumision(a);
        assertThrows(IllegalStateException.class, torneo::anularSorteo);
    }

    @Test
    void buscarSinAcentosNiMayusculasYPorVariasPalabras() {
        Participante juan = competidor("30111222", "Juan", "Pérez", 2000);
        Participante jose = competidor("28999000", "José", "Pereyra", 2000);
        torneo.inscribir(juan);
        torneo.inscribir(jose);

        assertEquals(List.of(juan, jose), torneo.buscar("PER"));
        assertEquals(List.of(juan), torneo.buscar("juan perez"));
        assertEquals(List.of(jose), torneo.buscar("jose"));
        assertEquals(List.of(jose), torneo.buscar("28999"));
        assertTrue(torneo.buscar("zzz").isEmpty());
        assertEquals(2, torneo.buscar("  ").size());
    }

    @Test
    void datosDePruebaGeneraUnTorneoSorteable() {
        DatosDePrueba.cargar(torneo, 80, new Random(1));
        assertEquals(80, torneo.getParticipantes().size());
        assertTrue(torneo.cantidadPesados() > 60);
        assertTrue(torneo.getParticipantes().stream().anyMatch(Participante::isExcedidoDePeso));
        torneo.sortear();
        assertFalse(torneo.getLlaves().isEmpty());
    }
}
