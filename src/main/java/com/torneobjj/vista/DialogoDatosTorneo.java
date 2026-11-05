package com.torneobjj.vista;

import com.torneobjj.servicio.Torneo;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Pide nombre y fecha del torneo. Sirve para crear uno nuevo (torneo == null)
 * o para editar el actual (los cambios se aplican al aceptar).
 */
class DialogoDatosTorneo extends Dialog<DialogoDatosTorneo.Datos> {

    record Datos(String nombre, LocalDate fecha) {
    }

    private final Torneo torneo;
    private final TextField nombre = new TextField();
    private final DatePicker fecha = new DatePicker();
    private final Mensaje error = new Mensaje();

    DialogoDatosTorneo(Torneo torneo, Scene escenaPadre) {
        this.torneo = torneo;
        setTitle(torneo == null ? "Nuevo torneo" : "Datos del torneo");
        setHeaderText(null);
        if (escenaPadre != null) {
            initOwner(escenaPadre.getWindow());
            getDialogPane().getStylesheets().addAll(escenaPadre.getStylesheets());
        }

        nombre.setText(torneo == null ? "" : torneo.getNombre());
        nombre.setPromptText("Ej.: Open de verano 2026");
        fecha.setValue(torneo == null ? LocalDate.now() : torneo.getFecha());
        fecha.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(LocalDate f) {
                return Formato.fecha(f);
            }

            @Override
            public LocalDate fromString(String s) {
                try {
                    return s == null || s.isBlank() ? null : LocalDate.parse(s.trim(), Formato.FECHA);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });

        Label titulo = new Label(torneo == null ? "Nuevo torneo" : "Datos del torneo");
        titulo.getStyleClass().add("titulo-tarjeta");
        GridPane grilla = new GridPane();
        grilla.getStyleClass().add("grilla-formulario");
        grilla.addRow(0, etiqueta("Nombre"), nombre);
        grilla.addRow(1, etiqueta("Fecha"), fecha);
        GridPane.setHgrow(nombre, Priority.ALWAYS);
        Label ayuda = new Label("La edad de cada competidor es la que cumple en el año del torneo. "
                + "La fecha no se puede cambiar después del sorteo.");
        ayuda.getStyleClass().add("ayuda");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox contenido = new VBox(14, titulo, grilla, ayuda, error);
        contenido.setPrefWidth(420);
        getDialogPane().setContent(contenido);

        ButtonType aceptar = new ButtonType(torneo == null ? "Crear" : "Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, aceptar);
        Button botonAceptar = (Button) getDialogPane().lookupButton(aceptar);
        botonAceptar.addEventFilter(ActionEvent.ACTION, e -> {
            if (!validarYAplicar()) {
                e.consume();
            }
        });
        setResultConverter(b -> b == aceptar ? new Datos(nombre.getText().trim(), leerFecha()) : null);
        nombre.requestFocus();
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta");
        return l;
    }

    private LocalDate leerFecha() {
        String texto = fecha.getEditor().getText();
        try {
            return texto == null || texto.isBlank() ? fecha.getValue() : LocalDate.parse(texto.trim(), Formato.FECHA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Valida los datos; si se está editando un torneo, aplica los cambios. */
    private boolean validarYAplicar() {
        try {
            if (nombre.getText().isBlank()) {
                throw new IllegalArgumentException("Escribí el nombre del torneo");
            }
            LocalDate f = leerFecha();
            if (f == null) {
                throw new IllegalArgumentException("Ingresá una fecha válida (dd/mm/aaaa)");
            }
            if (torneo != null) {
                torneo.cambiarFecha(f); // primero lo que puede fallar
                torneo.setNombre(nombre.getText());
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            error.error(e.getMessage());
            ajustarTamanio();
            return false;
        }
    }

    /** Agranda el diálogo para que entre el mensaje de error. */
    private void ajustarTamanio() {
        if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
            getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    // para pruebas
    void completar(String nuevoNombre, String nuevaFecha) {
        nombre.setText(nuevoNombre);
        fecha.getEditor().setText(nuevaFecha);
    }

    String getError() {
        return error.isVisible() ? error.getText() : "";
    }
}
