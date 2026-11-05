package com.torneobjj.vista;

import com.torneobjj.modelo.Participante;
import com.torneobjj.persistencia.ArchivoTorneo;
import com.torneobjj.servicio.DatosDePrueba;
import com.torneobjj.servicio.Torneo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Ventana principal: menú, encabezado con el resumen del torneo y una pestaña por etapa.
 * También maneja el archivo del torneo (nuevo, abrir, guardar) y los cambios sin guardar.
 */
public class VentanaPrincipal extends BorderPane {

    private Torneo torneo;
    private Path archivo;
    private boolean modificado;

    private final ReadOnlyStringWrapper titulo = new ReadOnlyStringWrapper();
    private final Label nombreTorneo = new Label();
    private final Label fechaTorneo = new Label();
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

        setTop(new VBox(menu(), encabezado()));
        setCenter(pestanias);
        refrescarTodo();
    }

    Torneo getTorneo() {
        return torneo;
    }

    /** Título para la ventana: nombre del torneo, archivo y * si hay cambios sin guardar. */
    public ReadOnlyStringProperty tituloProperty() {
        return titulo.getReadOnlyProperty();
    }

    public boolean hayCambiosSinGuardar() {
        return modificado;
    }

    /** Lo llaman las pantallas después de modificar el torneo. */
    void datosCambiaron() {
        modificado = true;
        refrescarTodo();
    }

    private void refrescarTodo() {
        nombreTorneo.setText(torneo.getNombre());
        fechaTorneo.setText("Fecha: " + Formato.fecha(torneo.getFecha())
                + "  ·  Edad: la que cumple cada competidor en " + torneo.getAnio());
        String estado = torneo.estaSorteado() ? "Llaves sorteadas" : "Sin sortear";
        resumen.setText(torneo.getParticipantes().size() + " inscriptos  ·  "
                + torneo.cantidadPesados() + " pesados  ·  " + estado);
        String nombreArchivo = archivo == null ? "sin guardar" : archivo.getFileName().toString();
        titulo.set((modificado ? "* " : "") + torneo.getNombre() + " – " + nombreArchivo
                + " – Simulador de llaves de Jiu-Jitsu");
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

    // ------------------------------------------------------------ archivo

    private void cambiarTorneo(Torneo nuevo, Path nuevoArchivo) {
        torneo = nuevo;
        archivo = nuevoArchivo;
        modificado = false;
        llaves.olvidarResaltado();
        seleccionarPestania(nuevo.estaSorteado() ? 2 : 0);
        refrescarTodo();
    }

    void nuevoTorneo() {
        if (!confirmarDescartarCambios()) {
            return;
        }
        DialogoDatosTorneo dialogo = new DialogoDatosTorneo(null, getScene());
        dialogo.showAndWait().ifPresent(datos -> cambiarTorneo(new Torneo(datos.nombre(), datos.fecha()), null));
    }

    void editarDatosTorneo() {
        DialogoDatosTorneo dialogo = new DialogoDatosTorneo(torneo, getScene());
        if (dialogo.showAndWait().isPresent()) {
            datosCambiaron();
        }
    }

    void abrir() {
        if (!confirmarDescartarCambios()) {
            return;
        }
        FileChooser selector = selectorDeArchivos("Abrir torneo");
        File elegido = selector.showOpenDialog(ventana());
        if (elegido != null) {
            abrir(elegido.toPath());
        }
    }

    /** Abre un archivo sin preguntar (lo usa también el menú y las pruebas). */
    public boolean abrir(Path ruta) {
        try {
            cambiarTorneo(ArchivoTorneo.abrir(ruta), ruta);
            return true;
        } catch (IOException e) {
            error("No se pudo abrir el torneo", e.getMessage());
            return false;
        }
    }

    /** Guarda en el archivo actual, o pregunta dónde si todavía no tiene. */
    public boolean guardar() {
        return archivo == null ? guardarComo() : guardarEn(archivo);
    }

    boolean guardarComo() {
        FileChooser selector = selectorDeArchivos("Guardar torneo");
        selector.setInitialFileName(nombreSugerido());
        File elegido = selector.showSaveDialog(ventana());
        return elegido != null && guardarEn(ArchivoTorneo.conExtension(elegido.toPath()));
    }

    public boolean guardarEn(Path ruta) {
        try {
            ArchivoTorneo.guardar(torneo, ruta);
            archivo = ruta;
            modificado = false;
            refrescarTodo();
            return true;
        } catch (IOException e) {
            error("No se pudo guardar el torneo", e.getMessage());
            return false;
        }
    }

    /**
     * Si hay cambios sin guardar, pregunta qué hacer.
     *
     * @return true si se puede seguir (se guardó o se eligió descartar), false si se canceló
     */
    public boolean confirmarDescartarCambios() {
        if (!modificado) {
            return true;
        }
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.YES);
        ButtonType descartar = new ButtonType("No guardar", ButtonBar.ButtonData.NO);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Querés guardar los cambios de «" + torneo.getNombre() + "»?", guardar, descartar, cancelar);
        alerta.setHeaderText("Hay cambios sin guardar");
        alerta.initOwner(ventana());
        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() == cancelar) {
            return false;
        }
        return respuesta.get() == descartar || guardar();
    }

    private FileChooser selectorDeArchivos(String tituloDialogo) {
        FileChooser selector = new FileChooser();
        selector.setTitle(tituloDialogo);
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Torneos (*" + ArchivoTorneo.EXTENSION + ")", "*" + ArchivoTorneo.EXTENSION),
                new FileChooser.ExtensionFilter("Archivos JSON (*.json)", "*.json"));
        File carpeta = archivo != null && archivo.toAbsolutePath().getParent() != null
                ? archivo.toAbsolutePath().getParent().toFile()
                : new File(System.getProperty("user.home"));
        if (carpeta.isDirectory()) {
            selector.setInitialDirectory(carpeta);
        }
        return selector;
    }

    private String nombreSugerido() {
        String base = torneo.getNombre().replaceAll("[\\\\/:*?\"<>|]", "").trim();
        return (base.isEmpty() ? "torneo" : base) + ArchivoTorneo.EXTENSION;
    }

    private Window ventana() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void error(String tituloError, String detalle) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, detalle);
        alerta.setHeaderText(tituloError);
        alerta.initOwner(ventana());
        alerta.showAndWait();
    }

    // ------------------------------------------------------------ armado

    private MenuBar menu() {
        MenuItem nuevo = item("Nuevo torneo…", "Shortcut+N", this::nuevoTorneo);
        MenuItem abrir = item("Abrir…", "Shortcut+O", this::abrir);
        MenuItem guardar = item("Guardar", "Shortcut+S", this::guardar);
        MenuItem guardarComo = item("Guardar como…", "Shortcut+Shift+S", this::guardarComo);
        MenuItem salir = item("Salir", null, () -> {
            Window w = ventana();
            if (w != null) {
                w.fireEvent(new javafx.stage.WindowEvent(w, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        });
        Menu menuArchivo = new Menu("Archivo", null, nuevo, abrir, new SeparatorMenuItem(),
                guardar, guardarComo, new SeparatorMenuItem(), salir);

        MenuItem datos = item("Nombre y fecha…", null, this::editarDatosTorneo);
        MenuItem prueba = item("Cargar 60 competidores de prueba", null, () -> {
            try {
                cargarDatosDePrueba(60, System.nanoTime());
            } catch (RuntimeException ex) {
                error("No se pudieron cargar los datos de prueba", ex.getMessage());
            }
        });
        Menu menuTorneo = new Menu("Torneo", null, datos, new SeparatorMenuItem(), prueba);

        MenuBar barra = new MenuBar(menuArchivo, menuTorneo);
        barra.getStyleClass().add("barra-menu");
        return barra;
    }

    private static MenuItem item(String texto, String atajo, Runnable accion) {
        MenuItem item = new MenuItem(texto);
        if (atajo != null) {
            item.setAccelerator(KeyCombination.keyCombination(atajo));
        }
        item.setOnAction(e -> accion.run());
        return item;
    }

    private HBox encabezado() {
        nombreTorneo.getStyleClass().add("titulo");
        fechaTorneo.getStyleClass().add("subtitulo");
        VBox textos = new VBox(2, nombreTorneo, fechaTorneo);
        resumen.getStyleClass().add("resumen");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox caja = new HBox(16, textos, espacio, resumen);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("encabezado");
        return caja;
    }

    private static Tab pestania(String titulo, Vista vista) {
        return new Tab(titulo, vista.getNodo());
    }

    /** Datos por defecto al abrir el programa. */
    static Torneo torneoInicial() {
        return new Torneo("Torneo de Jiu-Jitsu", LocalDate.now());
    }
}
