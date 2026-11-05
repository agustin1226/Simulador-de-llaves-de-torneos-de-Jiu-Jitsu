package com.torneobjj.vista;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Punto de entrada de la aplicación. Se ejecuta con {@code mvn javafx:run}. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        VentanaPrincipal ventana = new VentanaPrincipal(VentanaPrincipal.torneoInicial());
        stage.titleProperty().bind(ventana.tituloProperty());
        stage.setScene(crearEscena(ventana));
        // Antes de cerrar, ofrecer guardar los cambios
        stage.setOnCloseRequest(e -> {
            if (!ventana.confirmarDescartarCambios()) {
                e.consume();
            }
        });
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    static Scene crearEscena(VentanaPrincipal ventana) {
        Scene scene = new Scene(ventana, 1280, 800);
        scene.getStylesheets().add(App.class.getResource("estilos.css").toExternalForm());
        return scene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
