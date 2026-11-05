package com.torneobjj.vista;

import com.torneobjj.modelo.Participante;
import com.torneobjj.servicio.DatosDePrueba;
import com.torneobjj.servicio.Torneo;
import java.util.List;
import java.util.Random;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Ventana principal: encabezado con el resumen del torneo y una pestaña por etapa. */
public class VentanaPrincipal extends BorderPane {

    private final Torneo torneo;
    private final Label resumen = new Label();
    private final TabPane pestanias = new TabPane();
    private final VistaInscripcion inscripcion;
    private final VistaPesaje pesaje;
    private final VistaLlaves llaves;
    private final VistaBuscador buscador;
    private final List<Vista> vistas;

    public VentanaPrincipal(Torneo torneo) {
        this.torneo = torneo;
        getStyleClass().add("ventana");

        inscripcion = new VistaInscripcion(this);
        pesaje = new VistaPesaje(this);
        llaves = new VistaLlaves(this);
        buscador = new VistaBuscador(this);
        vistas = List.of(inscripcion, pesaje, llaves, buscador);

        pestanias.getTabs().addAll(
                pestania("1 · Inscripción", inscripcion),
                pestania("2 · Pesaje", pesaje),
                pestania("3 · Llaves", llaves),
                pestania("Buscador", buscador));
        pestanias.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestanias.getSelectionModel().selectedIndexProperty()
                .addListener((obs, viejo, nuevo) -> vistas.get(nuevo.intValue()).refrescar());

        setTop(encabezado());
        setCenter(pestanias);
        datosCambiaron();
    }

    Torneo getTorneo() {
        return torneo;
    }

    /** Lo llaman las pantallas después de modificar el torneo. */
    void datosCambiaron() {
        String estado = torneo.estaSorteado() ? "Llaves sorteadas" : "Sin sortear";
        resumen.setText(torneo.getParticipantes().size() + " inscriptos  ·  "
                + torneo.cantidadPesados() + " pesados  ·  " + estado);
        vistas.forEach(Vista::refrescar);
    }

    /** Va a la pestaña de llaves y resalta al competidor. */
    void irALlave(Participante p) {
        pestanias.getSelectionModel().select(2);
        llaves.mostrar(p);
    }

    public void seleccionarPestania(int indice) {
        pestanias.getSelectionModel().select(indice);
    }

    public void cargarDatosDePrueba(int cantidad, long semilla) {
        DatosDePrueba.cargar(torneo, cantidad, new Random(semilla));
        datosCambiaron();
    }

    private HBox encabezado() {
        Label titulo = new Label(torneo.getNombre());
        titulo.getStyleClass().add("titulo");
        Label fecha = new Label("Fecha: " + Formato.fecha(torneo.getFecha())
                + "  ·  Edad: la que cumple cada competidor en " + torneo.getAnio());
        fecha.getStyleClass().add("subtitulo");
        VBox textos = new VBox(2, titulo, fecha);

        resumen.getStyleClass().add("resumen");

        MenuItem prueba = new MenuItem("Cargar 60 competidores de prueba");
        prueba.setOnAction(e -> {
            try {
                cargarDatosDePrueba(60, System.nanoTime());
            } catch (RuntimeException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            }
        });
        MenuButton menu = new MenuButton("Opciones", null, prueba);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox caja = new HBox(16, textos, espacio, resumen, menu);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("encabezado");
        return caja;
    }

    private static Tab pestania(String titulo, Vista vista) {
        return new Tab(titulo, vista.getNodo());
    }
}
