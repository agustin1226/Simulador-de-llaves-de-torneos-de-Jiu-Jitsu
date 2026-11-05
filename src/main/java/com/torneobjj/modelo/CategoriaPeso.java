package com.torneobjj.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Categorías de peso IBJJF para adultos/masters, con kimono.
 * Los límites son "hasta X kg" inclusive.
 */
public enum CategoriaPeso {
    //            nombre           masculino           femenino
    GALLO        ("Gallo",         57.5,               48.5),
    PLUMA_LIGERO ("Pluma ligero",  64.0,               53.5),
    PLUMA        ("Pluma",         70.0,               58.5),
    LIGERO       ("Ligero",        76.0,               64.0),
    MEDIO        ("Medio",         82.3,               69.0),
    MEDIO_PESADO ("Medio pesado",  88.3,               74.0),
    PESADO       ("Pesado",        94.3,               79.3),
    SUPER_PESADO ("Súper pesado",  100.5,              Limite.SIN_LIMITE),
    PESADISIMO   ("Pesadísimo",    Limite.SIN_LIMITE,  Limite.NO_DISPONIBLE);

    private static final Locale ES = Locale.forLanguageTag("es-AR");

    private final String nombre;
    private final double limiteMasculino;
    private final double limiteFemenino;

    CategoriaPeso(String nombre, double limiteMasculino, double limiteFemenino) {
        this.nombre = nombre;
        this.limiteMasculino = limiteMasculino;
        this.limiteFemenino = limiteFemenino;
    }

    /** Las categorías que puede elegir un competidor de ese sexo, de la más liviana a la más pesada. */
    public static List<CategoriaPeso> disponiblesPara(Sexo sexo) {
        List<CategoriaPeso> lista = new ArrayList<>();
        for (CategoriaPeso c : values()) {
            if (c.disponiblePara(sexo)) {
                lista.add(c);
            }
        }
        return lista;
    }

    public boolean disponiblePara(Sexo sexo) {
        return !Double.isNaN(limiteCrudo(sexo));
    }

    public boolean tieneLimite(Sexo sexo) {
        return getLimite(sexo) != Limite.SIN_LIMITE;
    }

    /** Límite en kg, o {@code Double.POSITIVE_INFINITY} si la categoría no tiene máximo. */
    public double getLimite(Sexo sexo) {
        if (!disponiblePara(sexo)) {
            throw new IllegalArgumentException(
                    "La categoría " + nombre + " no existe para el sexo " + sexo);
        }
        return limiteCrudo(sexo);
    }

    /** true si el peso cumple con el límite de la categoría (el límite es inclusive). */
    public boolean admitePeso(Sexo sexo, double kg) {
        return kg <= getLimite(sexo);
    }

    /** Ej.: "Medio pesado (hasta 88,3 kg)" o "Súper pesado (sin límite)". */
    public String getDescripcion(Sexo sexo) {
        if (!tieneLimite(sexo)) {
            return nombre + " (sin límite)";
        }
        return String.format(ES, "%s (hasta %.1f kg)", nombre, getLimite(sexo));
    }

    private double limiteCrudo(Sexo sexo) {
        return sexo == Sexo.MASCULINO ? limiteMasculino : limiteFemenino;
    }

    @Override
    public String toString() {
        return nombre;
    }

    private static final class Limite {
        static final double SIN_LIMITE = Double.POSITIVE_INFINITY;
        static final double NO_DISPONIBLE = Double.NaN;
    }
}
