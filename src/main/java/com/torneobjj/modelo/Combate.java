package com.torneobjj.modelo;

import java.util.Objects;

/**
 * Combate entre dos competidores. El resultado se carga a mano:
 * por sumisión, o por puntos (con desempate automático y, si hace falta,
 * decisión del árbitro).
 */
public class Combate {

    private final Participante competidor1;
    private final Participante competidor2;
    private Resultado resultado;

    public Combate(Participante competidor1, Participante competidor2) {
        this.competidor1 = Objects.requireNonNull(competidor1);
        this.competidor2 = Objects.requireNonNull(competidor2);
        if (competidor1.equals(competidor2)) {
            throw new IllegalArgumentException("Un competidor no puede pelear contra sí mismo");
        }
    }

    public void registrarSumision(Participante ganador) {
        verificarPendiente();
        verificarParticipa(ganador);
        resultado = new Resultado(ganador, TipoVictoria.SUMISION, null, null);
    }

    /**
     * true si con ese marcador el combate queda empatado del todo
     * y lo tiene que definir el árbitro.
     */
    public boolean requiereDecisionArbitro(Marcador m1, Marcador m2) {
        return ReglaDesempate.decidir(competidor1, m1, competidor2, m2).isEmpty();
    }

    /** Registra el resultado por puntos. Falla si el empate requiere decisión del árbitro. */
    public void registrarPorPuntos(Marcador m1, Marcador m2) {
        verificarPendiente();
        ReglaDesempate.Decision decision = ReglaDesempate.decidir(competidor1, m1, competidor2, m2)
                .orElseThrow(() -> new IllegalStateException(
                        "Empate total: el ganador lo tiene que elegir el árbitro"));
        resultado = new Resultado(decision.ganador(), decision.tipo(), m1, m2);
    }

    /** Registra un empate total que define el árbitro. Solo válido si realmente hubo empate. */
    public void registrarDecisionArbitro(Marcador m1, Marcador m2, Participante elegido) {
        verificarPendiente();
        verificarParticipa(elegido);
        if (!requiereDecisionArbitro(m1, m2)) {
            throw new IllegalStateException("El marcador ya define un ganador; no corresponde decisión del árbitro");
        }
        resultado = new Resultado(elegido, TipoVictoria.DECISION_ARBITRO, m1, m2);
    }

    public boolean estaTerminado() {
        return resultado != null;
    }

    public Participante getGanador() {
        return resultado == null ? null : resultado.ganador();
    }

    public Participante getPerdedor() {
        if (resultado == null) {
            return null;
        }
        return resultado.ganador().equals(competidor1) ? competidor2 : competidor1;
    }

    public boolean participa(Participante p) {
        return competidor1.equals(p) || competidor2.equals(p);
    }

    public Participante getCompetidor1() { return competidor1; }
    public Participante getCompetidor2() { return competidor2; }
    public Resultado getResultado() { return resultado; }

    private void verificarPendiente() {
        if (estaTerminado()) {
            throw new IllegalStateException("El combate ya tiene resultado");
        }
    }

    private void verificarParticipa(Participante p) {
        if (p == null || !participa(p)) {
            throw new IllegalArgumentException("El ganador tiene que ser uno de los dos competidores");
        }
    }

    @Override
    public String toString() {
        return competidor1 + " vs " + competidor2;
    }
}
