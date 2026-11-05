package com.torneobjj.vista;

import com.torneobjj.llave.Llave;
import com.torneobjj.llave.Puesto;
import com.torneobjj.modelo.Participante;
import com.torneobjj.servicio.Torneo;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Búsqueda de competidores por nombre, apellido o DNI, con acceso directo a su llave. */
class VistaBuscador implements Vista {

    private final VentanaPrincipal ventana;
    private final VBox raiz = new VBox(14);
    private final TextField busqueda = new TextField();
    private final Label cantidad = new Label();
    private final TableView<Participante> tabla = Tablas.nueva("No se encontraron competidores");
    private final Button verLlave = new Button("Ver en la llave");

    VistaBuscador(VentanaPrincipal ventana) {
        this.ventana = ventana;
        raiz.getStyleClass().addAll("pagina", "tarjeta-pagina");

        Label titulo = new Label("Buscar competidor");
        titulo.getStyleClass().add("titulo-tarjeta");
        busqueda.setPromptText("Nombre, apellido o DNI (no importan mayúsculas ni acentos)");
        busqueda.getStyleClass().add("campo-busqueda");
        busqueda.textProperty().addListener((o, a, b) -> refrescar());
        busqueda.setOnAction(e -> irALlave());
        HBox.setHgrow(busqueda, Priority.ALWAYS);
        cantidad.getStyleClass().add("ayuda");

        verLlave.getStyleClass().add("boton-primario");
        verLlave.setOnAction(e -> irALlave());
        HBox barra = new HBox(12, busqueda, verLlave);
        barra.setAlignment(Pos.CENTER_LEFT);

        armarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(cantidad, espacio, ayuda("Doble clic o Enter para ver su llave"));

        VBox tarjeta = new VBox(12, titulo, barra, tabla, pie);
        tarjeta.getStyleClass().add("tarjeta");
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        raiz.getChildren().add(tarjeta);
    }

    private static Label ayuda(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("ayuda");
        return l;
    }

    private void armarTabla() {
        int anio = ventana.getTorneo().getAnio();
        tabla.getColumns().addAll(List.of(
                Tablas.<Participante, String>columna("Apellido y nombre",
                        p -> p.getApellido() + ", " + p.getNombre(), 180),
                Tablas.<Participante, String>columna("DNI", Participante::getDni, 90),
                Tablas.<Participante, String>columna("Categoría", p -> p.getCategoria(anio).getNombre(), 320),
                Tablas.<Participante, String>columna("Peso", p -> Formato.kg(p.getPesoOficial()), 80),
                Tablas.<Participante, String>columna("Pesaje", Formato::estadoPesaje, 130),
                Tablas.<Participante, String>columna("Llave", this::estadoEnLlave, 150)));

        tabla.setRowFactory(t -> {
            TableRow<Participante> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    irALlave();
                }
            });
            return fila;
        });
        verLlave.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
    }

    private String estadoEnLlave(Participante p) {
        Torneo torneo = ventana.getTorneo();
        if (!torneo.estaSorteado()) {
            return "Sin sortear";
        }
        Optional<Llave> llave = torneo.llaveDe(p);
        if (llave.isEmpty()) {
            return "Fuera (no se pesó)";
        }
        if (!llave.get().estaTerminada()) {
            return llave.get().rondaDeEliminacion(p)
                    .map(r -> "Eliminado en " + r.getNombre())
                    .orElse("En competencia");
        }
        return llave.get().getPodio().stream()
                .filter(puesto -> puesto.participante().equals(p))
                .map(Puesto::lugar)
                .findFirst()
                .map(lugar -> lugar + "° puesto")
                .orElse("Eliminado");
    }

    private void irALlave() {
        Participante p = tabla.getSelectionModel().getSelectedItem();
        if (p == null && tabla.getItems().size() == 1) {
            p = tabla.getItems().get(0);
        }
        if (p != null) {
            ventana.irALlave(p);
        }
    }

    @Override
    public void refrescar() {
        Torneo torneo = ventana.getTorneo();
        List<Participante> resultados = torneo.buscar(busqueda.getText());
        Participante antes = tabla.getSelectionModel().getSelectedItem();
        tabla.setItems(FXCollections.observableArrayList(resultados));
        if (antes != null && resultados.contains(antes)) {
            tabla.getSelectionModel().select(antes);
        }
        tabla.refresh(); // el estado en la llave cambia aunque la lista sea la misma
        cantidad.setText(resultados.size() + " de " + torneo.getParticipantes().size() + " competidores");
    }

    /** Para pruebas: escribe un texto en el buscador. */
    void buscar(String texto) {
        busqueda.setText(texto);
    }

    @Override
    public Node getNodo() {
        return raiz;
    }
}
