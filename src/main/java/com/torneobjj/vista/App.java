package com.torneobjj.vista;

import com.torneobjj.servicio.Torneo;
import java.time.LocalDate;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Punto de entrada de la aplicación. Se ejecuta con {@code mvn javafx:run}. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        Torneo torneo = new Torneo("Torneo de Jiu-Jitsu", LocalDate.now());
        stage.setTitle("Simulador de llaves – Jiu-Jitsu");
        stage.setScene(crearEscena(new VentanaPrincipal(torneo)));
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
