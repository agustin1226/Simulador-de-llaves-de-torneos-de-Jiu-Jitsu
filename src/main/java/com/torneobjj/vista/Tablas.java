package com.torneobjj.vista;

import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Ayudas para armar tablas con menos código. */
final class Tablas {

    private Tablas() {
    }

    static <S, T> TableColumn<S, T> columna(String titulo, Function<S, T> valor, double ancho) {
        TableColumn<S, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(valor.apply(d.getValue())));
        c.setPrefWidth(ancho);
        return c;
    }

    static <S> TableView<S> nueva(String textoVacia) {
        TableView<S> tabla = new TableView<>();
        tabla.setPlaceholder(new Label(textoVacia));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tabla;
    }
}
