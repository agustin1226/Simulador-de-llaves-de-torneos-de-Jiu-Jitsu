package com.torneobjj.modelo;

import java.util.Optional;

/**
 * Decide el ganador de un combate por puntos.
 * Orden: puntos (más) → ventajas (más) → penalidades (menos) →
 * si sigue empatado, pierde el excedido de peso (si solo uno lo está).
 * Si nada lo define, devuelve vacío: decide el árbitro.
 */
public final class ReglaDesempate {

    public record Decision(Participante ganador, TipoVictoria tipo) {
    }

    private ReglaDesempate() {
    }

    public static Optional<Decision> decidir(Participante p1, Marcador m1, Participante p2, Marcador m2) {
        if (m1.puntos() != m2.puntos()) {
            return Optional.of(new Decision(m1.puntos() > m2.puntos() ? p1 : p2, TipoVictoria.PUNTOS));
        }
        if (m1.ventajas() != m2.ventajas()) {
            return Optional.of(new Decision(m1.ventajas() > m2.ventajas() ? p1 : p2, TipoVictoria.VENTAJAS));
        }
        if (m1.penalidades() != m2.penalidades()) {
            return Optional.of(new Decision(m1.penalidades() < m2.penalidades() ? p1 : p2, TipoVictoria.PENALIDADES));
        }
        boolean excedido1 = p1.isExcedidoDePeso();
        boolean excedido2 = p2.isExcedidoDePeso();
        if (excedido1 != excedido2) {
            return Optional.of(new Decision(excedido1 ? p2 : p1, TipoVictoria.PESO));
        }
        return Optional.empty();
    }
}
