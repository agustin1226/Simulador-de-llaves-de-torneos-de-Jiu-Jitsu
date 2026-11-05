package com.torneobjj.llave;

import static org.junit.jupiter.api.Assertions.*;

import com.torneobjj.modelo.Categoria;
import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.DivisionEdad;
import com.torneobjj.modelo.Nivel;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Sexo;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SorteoTest {

    private static int dni = 0;

    private static Participante inscripto(int anioNacimiento, Sexo sexo, Nivel nivel, CategoriaPeso peso, Double kg) {
        Participante p = new Participante("D" + (++dni), "N", "A", LocalDate.of(anioNacimiento, 6, 1),
                sexo, nivel, peso);
        if (kg != null) {
            p.registrarPeso(kg);
        }
        return p;
    }

    @Test
    void agrupaPorCategoriaYDejaAfueraALosQueNoSePesaron() {
        Participante adulto1 = inscripto(2000, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, 85.0);
        Participante adulto2 = inscripto(2001, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, 87.0);
        Participante sinPesar = inscripto(2002, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, null);
        Participante master = inscripto(1993, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, 86.0);
        Participante otroNivel = inscripto(2000, Sexo.MASCULINO, Nivel.PROFESIONAL, CategoriaPeso.MEDIO_PESADO, 86.0);
        Participante mujer = inscripto(2000, Sexo.FEMENINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, 73.0);

        Map<Categoria, Llave> llaves = Sorteo.sortear(
                List.of(mujer, otroNivel, adulto1, sinPesar, master, adulto2), 2026);

        assertEquals(List.of(
                new Categoria(Sexo.MASCULINO, DivisionEdad.ADULTO, CategoriaPeso.MEDIO_PESADO, Nivel.AMATEUR),
                new Categoria(Sexo.MASCULINO, DivisionEdad.ADULTO, CategoriaPeso.MEDIO_PESADO, Nivel.PROFESIONAL),
                new Categoria(Sexo.MASCULINO, DivisionEdad.MASTER_1, CategoriaPeso.MEDIO_PESADO, Nivel.AMATEUR),
                new Categoria(Sexo.FEMENINO, DivisionEdad.ADULTO, CategoriaPeso.MEDIO_PESADO, Nivel.AMATEUR)),
                List.copyOf(llaves.keySet()));

        Llave adultos = llaves.values().iterator().next();
        assertEquals(List.of(adulto1, adulto2), adultos.getParticipantes());
        assertFalse(llaves.values().stream().anyMatch(l -> l.contiene(sinPesar)));
    }

    @Test
    void elExcedidoDePesoIgualEntraEnLaLlave() {
        Participante excedido = inscripto(2000, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, 90.0);
        Participante normal = inscripto(2000, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.MEDIO_PESADO, 85.0);
        Map<Categoria, Llave> llaves = Sorteo.sortear(List.of(excedido, normal), 2026);
        assertTrue(llaves.values().iterator().next().contiene(excedido));
    }

    @Test
    void sinPesadosNoHayLlaves() {
        Participante p = inscripto(2000, Sexo.MASCULINO, Nivel.AMATEUR, CategoriaPeso.GALLO, null);
        assertTrue(Sorteo.sortear(List.of(p), 2026).isEmpty());
    }
}
