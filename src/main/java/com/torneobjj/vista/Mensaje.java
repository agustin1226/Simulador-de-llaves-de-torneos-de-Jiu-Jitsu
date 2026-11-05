package com.torneobjj.vista;

import javafx.scene.control.Label;

/** Etiqueta para mostrar avisos de error, éxito o advertencia debajo de un formulario. */
class Mensaje extends Label {

    Mensaje() {
        setWrapText(true);
        getStyleClass().add("mensaje");
        setManaged(false);
        setVisible(false);
    }

    void error(String texto) { mostrar(texto, "mensaje-error"); }
    void exito(String texto) { mostrar(texto, "mensaje-exito"); }
    void advertencia(String texto) { mostrar(texto, "mensaje-advertencia"); }

    void ocultar() {
        setManaged(false);
        setVisible(false);
    }

    private void mostrar(String texto, String estilo) {
        getStyleClass().removeAll("mensaje-error", "mensaje-exito", "mensaje-advertencia");
        getStyleClass().add(estilo);
        setText(texto);
        setManaged(true);
        setVisible(true);
    }
}
