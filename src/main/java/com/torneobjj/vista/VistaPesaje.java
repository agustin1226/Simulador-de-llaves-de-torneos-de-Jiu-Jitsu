package com.torneobjj.vista;

import com.torneobjj.modelo.Participante;
import com.torneobjj.servicio.Torneo;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Registro del peso oficial. Si alguien se pasa del límite igual pelea,
 * pero si el combate termina empatado lo pierde.
 */
class VistaPesaje implements Vista {

    private final VentanaPrincipal ventana;
    private final BorderPane raiz = new BorderPane();
    private final TextField filtro = new TextField();
    private final Label contador = new Label();
    private final TableView<Participante> tabla = Tablas.nueva("No hay competidores inscriptos");

    private final Label seleccionado = new Label("Elegí un competidor de la tabla");
    private final Label limite = new Label();
    private final TextField peso = new TextField();
    private final Button registrar = new Button("Registrar peso");
    private final Mensaje mensaje = new Mensaje();
    private final Label avisoSorteo = new Label("Las llaves ya fueron sorteadas con estos pesos: "
            + "no se pueden modificar. Para hacerlo, anulá el sorteo desde la pestaña Llaves.");

    VistaPesaje(VentanaPrincipal ventana) {
        this.ventana = ventana;
        raiz.getStyleClass().add("pagina");

        filtro.setPromptText("Buscar por nombre, apellido o DNI…");
        filtro.setPrefWidth(320);
        filtro.textProperty().addListener((o, a, b) -> refrescar());
        contador.getStyleClass().add("ayuda");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(12, filtro, espacio, contador);
        barra.setAlignment(Pos.CENTER_LEFT);

        armarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox listado = new VBox(12, barra, tabla);
        listado.getStyleClass().add("tarjeta");

        raiz.setCenter(listado);
        raiz.setRight(formulario());
        BorderPane.setMargin(raiz.getRight(), new javafx.geometry.Insets(0, 0, 0, 20));
    }

    private void armarTabla() {
        tabla.getColumns().addAll(List.of(
                Tablas.<Participante, String>columna("Apellido y nombre",
                        p -> p.getApellido() + ", " + p.getNombre(), 170),
                Tablas.<Participante, String>columna("DNI", Participante::getDni, 85),
                Tablas.<Participante, String>columna("Categoría",
                        p -> p.getCategoria(ventana.getTorneo().getAnio()).getNombre(), 300),
                Tablas.<Participante, String>columna("Límite",
                        p -> Formato.limite(p.getSexo(), p.getCategoriaPeso()), 80),
                Tablas.<Participante, String>columna("Peso oficial", p -> Formato.kg(p.getPesoOficial()), 90),
                Tablas.<Participante, String>columna("Estado", Formato::estadoPesaje, 130)));

        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Participante p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().removeAll("fila-excedida", "fila-en-peso");
                if (!empty && p != null && p.estaPesado()) {
                    getStyleClass().add(p.isExcedidoDePeso() ? "fila-excedida" : "fila-en-peso");
                }
            }
        });
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> mostrarSeleccionado(p));
    }

    private Node formulario() {
        Label titulo = new Label("Registrar peso");
        titulo.getStyleClass().add("titulo-tarjeta");
        seleccionado.getStyleClass().add("nombre-destacado");
        seleccionado.setWrapText(true);
        limite.getStyleClass().add("ayuda");
        limite.setWrapText(true);

        peso.setPromptText("Ej.: 84,6");
        peso.setOnAction(e -> registrar());
        registrar.getStyleClass().add("boton-primario");
        registrar.setOnAction(e -> registrar());
        registrar.setMaxWidth(Double.MAX_VALUE);
        HBox fila = new HBox(8, peso, new Label("kg"));
        fila.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(peso, Priority.ALWAYS);

        Label regla = new Label("Si se pasa del límite igual pelea, pero si el combate termina "
                + "empatado lo pierde.");
        regla.getStyleClass().add("ayuda");
        regla.setWrapText(true);

        avisoSorteo.setWrapText(true);
        avisoSorteo.getStyleClass().addAll("mensaje", "mensaje-advertencia");

        VBox tarjeta = new VBox(12, titulo, avisoSorteo, seleccionado, limite, fila, registrar, mensaje, regla);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPrefWidth(300);
        tarjeta.setMinWidth(280);
        mostrarSeleccionado(null);
        return tarjeta;
    }

    private void mostrarSeleccionado(Participante p) {
        boolean bloqueado = p == null || ventana.getTorneo().estaSorteado();
        peso.setDisable(bloqueado);
        registrar.setDisable(bloqueado);
        if (p == null) {
            seleccionado.setText("Elegí un competidor de la tabla");
            limite.setText("");
            peso.clear();
            return;
        }
        seleccionado.setText(p.getNombreCompleto());
        limite.setText(p.getCategoriaPeso().getDescripcion(p.getSexo()) + "\n" + p.getSexo() + " · " + p.getNivel());
        peso.setText(p.estaPesado() ? String.format(Formato.ES, "%.1f", p.getPesoOficial()) : "");
        if (!bloqueado) {
            peso.requestFocus();
            peso.selectAll();
        }
    }

    private void registrar() {
        Participante p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        Torneo torneo = ventana.getTorneo();
        try {
            torneo.registrarPeso(p, Formato.parsearKg(peso.getText()));
            if (p.isExcedidoDePeso()) {
                mensaje.advertencia(p.getNombreCompleto() + ": " + Formato.estadoPesaje(p)
                        + ". Pelea igual, pero si empata pierde.");
            } else {
                mensaje.exito(p.getNombreCompleto() + ": " + Formato.kg(p.getPesoOficial()) + ", en peso.");
            }
            ventana.datosCambiaron();
            seleccionarSiguienteSinPesar(p);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje.error(ex.getMessage());
        }
    }

    /** Para pesar rápido: después de registrar, salta al próximo que falta pesar. */
    private void seleccionarSiguienteSinPesar(Participante actual) {
        List<Participante> items = tabla.getItems();
        int desde = items.indexOf(actual);
        for (int i = 1; i <= items.size(); i++) {
            Participante p = items.get((desde + i) % items.size());
            if (!p.estaPesado()) {
                tabla.getSelectionModel().select(p);
                tabla.scrollTo(p);
                return;
            }
        }
        tabla.getSelectionModel().select(actual);
    }

    @Override
    public void refrescar() {
        Torneo torneo = ventana.getTorneo();
        boolean sorteado = torneo.estaSorteado();
        avisoSorteo.setVisible(sorteado);
        avisoSorteo.setManaged(sorteado);

        Participante seleccionadoAntes = tabla.getSelectionModel().getSelectedItem();
        List<Participante> lista = filtro.getText().isBlank()
                ? torneo.getParticipantes()
                : torneo.buscar(filtro.getText());
        tabla.setItems(FXCollections.observableArrayList(lista));
        if (seleccionadoAntes != null && lista.contains(seleccionadoAntes)) {
            tabla.getSelectionModel().select(seleccionadoAntes);
        }
        mostrarSeleccionado(tabla.getSelectionModel().getSelectedItem());
        tabla.refresh();

        long excedidos = torneo.getParticipantes().stream().filter(Participante::isExcedidoDePeso).count();
        contador.setText("Pesados: " + torneo.cantidadPesados() + " de " + torneo.getParticipantes().size()
                + (excedidos > 0 ? "  ·  Excedidos: " + excedidos : ""));
    }

    @Override
    public Node getNodo() {
        return raiz;
    }
}
