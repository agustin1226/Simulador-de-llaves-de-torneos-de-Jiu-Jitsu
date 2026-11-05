package com.torneobjj.modelo;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Competidor inscripto. Se identifica por su DNI.
 * El peso oficial queda en null hasta que pasa por el pesaje.
 */
public class Participante {

    public static final double PESO_MINIMO_VALIDO = 30.0;
    public static final double PESO_MAXIMO_VALIDO = 250.0;

    private final String dni;
    private final String nombre;
    private final String apellido;
    private final LocalDate fechaNacimiento;
    private final Sexo sexo;
    private final Nivel nivel;
    private final CategoriaPeso categoriaPeso;
    private Double pesoOficial;

    public Participante(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
                        Sexo sexo, Nivel nivel, CategoriaPeso categoriaPeso) {
        this.dni = textoObligatorio(dni, "DNI");
        this.nombre = textoObligatorio(nombre, "nombre");
        this.apellido = textoObligatorio(apellido, "apellido");
        this.fechaNacimiento = Objects.requireNonNull(fechaNacimiento, "La fecha de nacimiento es obligatoria");
        this.sexo = Objects.requireNonNull(sexo, "El sexo es obligatorio");
        this.nivel = Objects.requireNonNull(nivel, "El nivel es obligatorio");
        this.categoriaPeso = Objects.requireNonNull(categoriaPeso, "La categoría de peso es obligatoria");
        if (!categoriaPeso.disponiblePara(sexo)) {
            throw new IllegalArgumentException(
                    "La categoría " + categoriaPeso + " no existe para el sexo " + sexo);
        }
    }

    /** Edad que cumple en el año del torneo (criterio IBJJF). */
    public int getEdadEnAnio(int anioTorneo) {
        return anioTorneo - fechaNacimiento.getYear();
    }

    /** Categoría completa para un torneo de ese año. Falla si es menor de 18. */
    public Categoria getCategoria(int anioTorneo) {
        int edad = getEdadEnAnio(anioTorneo);
        DivisionEdad division = DivisionEdad.desdeEdad(edad).orElseThrow(() ->
                new IllegalStateException(getNombreCompleto() + " tiene " + edad
                        + " años; la edad mínima es " + DivisionEdad.EDAD_MINIMA));
        return new Categoria(sexo, division, categoriaPeso, nivel);
    }

    public void registrarPeso(double kg) {
        if (kg < PESO_MINIMO_VALIDO || kg > PESO_MAXIMO_VALIDO) {
            throw new IllegalArgumentException("Peso fuera de rango: " + kg + " kg");
        }
        this.pesoOficial = kg;
    }

    public boolean estaPesado() {
        return pesoOficial != null;
    }

    /** true si ya se pesó y superó el límite de su categoría. */
    public boolean isExcedidoDePeso() {
        return estaPesado() && !categoriaPeso.admitePeso(sexo, pesoOficial);
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public String getDni() { return dni; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public Sexo getSexo() { return sexo; }
    public Nivel getNivel() { return nivel; }
    public CategoriaPeso getCategoriaPeso() { return categoriaPeso; }
    /** Peso registrado en el pesaje, o null si todavía no se pesó. */
    public Double getPesoOficial() { return pesoOficial; }

    private static String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        return valor.trim();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Participante p && dni.equals(p.dni);
    }

    @Override
    public int hashCode() {
        return dni.hashCode();
    }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
