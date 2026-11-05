package com.torneobjj.llave;

import com.torneobjj.modelo.Categoria;
import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.DivisionEdad;
import com.torneobjj.modelo.Nivel;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Sexo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Ayudas para los tests de llaves. */
final class Competidores {

    static final Categoria CATEGORIA =
            new Categoria(Sexo.MASCULINO, DivisionEdad.ADULTO, CategoriaPeso.MEDIO_PESADO, Nivel.AMATEUR);

    private Competidores() {
    }

    /** Competidor pesado con ese peso; el DNI es "P" + el peso para reconocerlo en los tests. */
    static Participante conPeso(double kg) {
        Participante p = new Participante("P" + kg, "Peso", String.valueOf(kg),
                LocalDate.of(2000, 1, 1), Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO);
        p.registrarPeso(kg);
        return p;
    }

    static List<Participante> conPesos(double... pesos) {
        List<Participante> lista = new ArrayList<>();
        for (double kg : pesos) {
            lista.add(conPeso(kg));
        }
        return lista;
    }

    static Llave llave(double... pesos) {
        return new Llave(CATEGORIA, conPesos(pesos));
    }

    /** Gana siempre el más pesado del combate. */
    static void ganaElMasPesado(Combate c) {
        boolean primero = c.getCompetidor1().getPesoOficial() > c.getCompetidor2().getPesoOficial();
        c.registrarSumision(primero ? c.getCompetidor1() : c.getCompetidor2());
    }

    /** Gana siempre el más liviano del combate. */
    static void ganaElMasLiviano(Combate c) {
        boolean primero = c.getCompetidor1().getPesoOficial() < c.getCompetidor2().getPesoOficial();
        c.registrarSumision(primero ? c.getCompetidor1() : c.getCompetidor2());
    }

    static void ganaAlAzar(Combate c, Random random) {
        c.registrarSumision(random.nextBoolean() ? c.getCompetidor1() : c.getCompetidor2());
    }
}
