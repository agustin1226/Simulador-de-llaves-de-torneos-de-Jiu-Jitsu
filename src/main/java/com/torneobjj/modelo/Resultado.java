package com.torneobjj.modelo;

/**
 * Resultado de un combate terminado.
 * Los marcadores son null si terminó por sumisión.
 */
public record Resultado(Participante ganador, TipoVictoria tipo, Marcador marcador1, Marcador marcador2) {
}
