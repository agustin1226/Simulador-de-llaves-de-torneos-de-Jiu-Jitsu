package com.torneobjj.servicio;

import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.DivisionEdad;
import com.torneobjj.modelo.Nivel;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Sexo;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Genera competidores al azar para probar la aplicación.
 * Los crea en grupos de la misma categoría para que haya llaves de distintos tamaños.
 */
public final class DatosDePrueba {

    private static final List<String> NOMBRES_M = List.of("Juan", "Martín", "Lucas", "Mateo", "Santiago",
            "Nicolás", "Facundo", "Tomás", "Agustín", "Joaquín", "Federico", "Diego", "Pablo", "Gonzalo",
            "Ignacio", "Rodrigo", "Franco", "Bruno", "Leandro", "Matías");
    private static final List<String> NOMBRES_F = List.of("Sofía", "Valentina", "Camila", "Lucía", "Martina",
            "Julieta", "Florencia", "Agustina", "Micaela", "Carolina", "Paula", "Rocío");
    private static final List<String> APELLIDOS = List.of("González", "Rodríguez", "Gómez", "Fernández",
            "López", "Díaz", "Martínez", "Pérez", "García", "Sánchez", "Romero", "Sosa", "Torres", "Álvarez",
            "Ruiz", "Ramírez", "Flores", "Benítez", "Acosta", "Medina", "Herrera", "Suárez", "Aguirre",
            "Giménez", "Molina", "Silva", "Castro", "Rojas", "Ortiz", "Núñez");

    private DatosDePrueba() {
    }

    /**
     * Inscribe {@code cantidad} competidores al azar y pesa a la mayoría
     * (algunos quedan sin pesar y unos pocos se pasan de peso).
     */
    public static void cargar(Torneo torneo, int cantidad, Random random) {
        int creados = 0;
        while (creados < cantidad) {
            Sexo sexo = random.nextInt(5) == 0 ? Sexo.FEMENINO : Sexo.MASCULINO;
            DivisionEdad division = random.nextInt(3) == 0
                    ? DivisionEdad.values()[1 + random.nextInt(3)]
                    : DivisionEdad.ADULTO;
            List<CategoriaPeso> pesos = CategoriaPeso.disponiblesPara(sexo);
            CategoriaPeso peso = pesos.get(2 + random.nextInt(pesos.size() - 4));
            Nivel nivel = Nivel.values()[random.nextInt(Nivel.values().length)];

            int grupo = Math.min(cantidad - creados, 1 + random.nextInt(8));
            for (int i = 0; i < grupo; i++) {
                Participante p = crear(torneo, sexo, division, peso, nivel, random);
                torneo.inscribir(p);
                if (random.nextInt(12) != 0) {
                    torneo.registrarPeso(p, pesoAlAzar(sexo, peso, random));
                }
                creados++;
            }
        }
    }

    private static Participante crear(Torneo torneo, Sexo sexo, DivisionEdad division, CategoriaPeso peso,
                                      Nivel nivel, Random random) {
        String dni;
        do {
            dni = String.valueOf(20_000_000 + random.nextInt(25_000_000));
        } while (torneo.buscarPorDni(dni).isPresent());

        int edadMax = Math.min(division.getEdadMaxima(), 70);
        int edad = division.getEdadMinima() + random.nextInt(edadMax - division.getEdadMinima() + 1);
        LocalDate nacimiento = LocalDate.of(torneo.getAnio() - edad, 1 + random.nextInt(12), 1 + random.nextInt(28));

        List<String> nombres = sexo == Sexo.MASCULINO ? NOMBRES_M : NOMBRES_F;
        return new Participante(dni, nombres.get(random.nextInt(nombres.size())),
                APELLIDOS.get(random.nextInt(APELLIDOS.size())), nacimiento, sexo, nivel, peso);
    }

    /** Un peso hasta 5 kg por debajo del límite; 1 de cada 15 se pasa por hasta 1,5 kg. */
    private static double pesoAlAzar(Sexo sexo, CategoriaPeso peso, Random random) {
        double limite = peso.getLimite(sexo);
        double kg = random.nextInt(15) == 0
                ? limite + 0.1 + random.nextDouble() * 1.4
                : limite - random.nextDouble() * 5;
        return Math.round(kg * 10) / 10.0;
    }
}
