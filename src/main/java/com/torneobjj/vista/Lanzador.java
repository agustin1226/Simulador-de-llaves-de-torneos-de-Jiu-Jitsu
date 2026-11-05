package com.torneobjj.vista;

/**
 * Main alternativo para ejecutar desde el IDE sin configurar módulos:
 * Java no deja arrancar directamente una clase que extiende Application
 * si JavaFX está en el classpath en lugar del module path.
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
