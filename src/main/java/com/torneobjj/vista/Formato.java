package com.torneobjj.vista;

import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Sexo;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de texto compartidos por las pantallas. */
final class Formato {

    static final Locale ES = Locale.forLanguageTag("es-AR");
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formato() {
    }

    static String kg(Double kg) {
        return kg == null ? "—" : String.format(ES, "%.1f kg", kg);
    }

    static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    static String limite(Sexo sexo, CategoriaPeso peso) {
        return peso.tieneLimite(sexo) ? kg(peso.getLimite(sexo)) : "Sin límite";
    }

    static String estadoPesaje(Participante p) {
        if (!p.estaPesado()) {
            return "Sin pesar";
        }
        if (p.isExcedidoDePeso()) {
            double exceso = p.getPesoOficial() - p.getCategoriaPeso().getLimite(p.getSexo());
            return String.format(ES, "Excedido (+%.1f kg)", exceso);
        }
        return "En peso";
    }

    /** Acepta "85,3" o "85.3". */
    static double parsearKg(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Ingresá el peso");
        }
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Peso inválido: " + texto);
        }
    }
}
