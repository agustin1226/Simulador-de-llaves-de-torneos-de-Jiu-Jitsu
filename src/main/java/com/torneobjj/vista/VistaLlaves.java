package com.torneobjj.vista;

import com.torneobjj.llave.Llave;
import com.torneobjj.llave.Puesto;
import com.torneobjj.llave.Ronda;
import com.torneobjj.modelo.Categoria;
import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Marcador;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Resultado;
import com.torneobjj.modelo.TipoVictoria;
import com.torneobjj.servicio.Torneo;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Sorteo y visualización de las llaves, una categoría a la vez. */
class VistaLlaves implements Vista {

    private final VentanaPrincipal ventana;
    private final BorderPane raiz = new BorderPane();
    private final Button sortear = new Button("Sortear llaves");
    private final Button anular = new Button("Anular sorteo");
    private final Label info = new Label();
    private final ListView<Categoria> categorias = new ListView<>();
    private final ScrollPane contenedorLlave = new ScrollPane();

    /** Competidor a resaltar (el que se buscó en el buscador). */
    private Participante resaltado;

    VistaLlaves(VentanaPrincipal ventana) {
        this.ventana = ventana;
        raiz.getStyleClass().add("pagina");

        sortear.getStyleClass().add("boton-primario");
        sortear.setOnAction(e -> sortear());
        anular.setOnAction(e -> anular());
        info.getStyleClass().add("ayuda");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(10, sortear, anular, espacio, info);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("barra-acciones");

        categorias.setCellFactory(lv -> new CeldaCategoria());
        categorias.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> dibujarLlave());
        categorias.setPrefWidth(340);
        categorias.setMinWidth(280);
        Label tituloCategorias = new Label("Categorías");
        tituloCategorias.getStyleClass().add("titulo-tarjeta");
        VBox.setVgrow(categorias, Priority.ALWAYS);
        VBox panelCategorias = new VBox(10, tituloCategorias, categorias);
        panelCategorias.getStyleClass().add("tarjeta");

        contenedorLlave.setFitToHeight(true);
        contenedorLlave.getStyleClass().add("contenedor-llave");

        raiz.setTop(barra);
        raiz.setLeft(panelCategorias);
        raiz.setCenter(contenedorLlave);
        BorderPane.setMargin(contenedorLlave, new javafx.geometry.Insets(0, 0, 0, 20));
    }

    // ------------------------------------------------------------ acciones

    private void sortear() {
        Torneo torneo = ventana.getTorneo();
        List<Participante> ausentes = torneo.getAusentes();
        String texto = "Se van a armar las llaves con los " + torneo.cantidadPesados() + " competidores pesados.";
        if (!ausentes.isEmpty()) {
            texto += "\n\n" + ausentes.size() + " inscriptos no se pesaron y quedan afuera.";
        }
        texto += "\n\nDespués del sorteo no se puede inscribir ni pesar (salvo que se anule).";
        if (!confirmar("Sortear llaves", texto)) {
            return;
        }
        try {
            torneo.sortear();
            resaltado = null;
            ventana.datosCambiaron();
            categorias.getSelectionModel().selectFirst();
        } catch (IllegalStateException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    private void anular() {
        if (!confirmar("Anular sorteo", "Se borran todas las llaves para poder volver a inscribir o pesar.")) {
            return;
        }
        try {
            ventana.getTorneo().anularSorteo();
            resaltado = null;
            ventana.datosCambiaron();
        } catch (IllegalStateException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    private static boolean confirmar(String titulo, String texto) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        return alerta.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /** Selecciona la categoría del competidor y lo resalta en la llave. */
    void mostrar(Participante p) {
        resaltado = p;
        ventana.getTorneo().llaveDe(p).ifPresentOrElse(llave -> {
            categorias.getSelectionModel().select(llave.getCategoria());
            categorias.scrollTo(llave.getCategoria());
            dibujarLlave();
        }, this::dibujarLlave);
    }

    // ------------------------------------------------------------ refresco

    @Override
    public void refrescar() {
        Torneo torneo = ventana.getTorneo();
        boolean sorteado = torneo.estaSorteado();
        sortear.setDisable(sorteado);
        anular.setDisable(!sorteado);

        if (sorteado) {
            int ausentes = torneo.getAusentes().size();
            info.setText(torneo.getLlaves().size() + " categorías"
                    + (ausentes > 0 ? "  ·  " + ausentes + " sin pesar (fuera del sorteo)" : ""));
        } else {
            info.setText(torneo.cantidadPesados() + " competidores pesados listos para sortear");
        }

        Categoria seleccionada = categorias.getSelectionModel().getSelectedItem();
        categorias.setItems(FXCollections.observableArrayList(torneo.getLlaves().keySet()));
        if (seleccionada != null && torneo.getLlaves().containsKey(seleccionada)) {
            categorias.getSelectionModel().select(seleccionada);
        } else if (sorteado) {
            categorias.getSelectionModel().selectFirst();
        }
        categorias.refresh();
        dibujarLlave();
    }

    // ------------------------------------------------------------ dibujo de la llave

    private void dibujarLlave() {
        Torneo torneo = ventana.getTorneo();
        if (!torneo.estaSorteado()) {
            contenedorLlave.setContent(vacio("Todavía no se sortearon las llaves",
                    "Inscribí y pesá a los competidores, y después apretá «Sortear llaves».\n"
                            + "Los que no se pesaron quedan fuera del sorteo."));
            return;
        }
        Categoria categoria = categorias.getSelectionModel().getSelectedItem();
        if (categoria == null) {
            String texto = resaltado != null
                    ? resaltado.getNombreCompleto() + " no está en ninguna llave (no se pesó)."
                    : "Elegí una categoría de la lista.";
            contenedorLlave.setContent(vacio("Sin categoría seleccionada", texto));
            return;
        }
        Llave llave = torneo.getLlaves().get(categoria);

        Label titulo = new Label(categoria.getNombre());
        titulo.getStyleClass().add("titulo-llave");
        Label subtitulo = new Label(competidores(llave.getParticipantes().size()) + "  ·  " + descripcionFormato(llave));
        subtitulo.getStyleClass().add("ayuda");

        HBox rondas = new HBox(24);
        rondas.getStyleClass().add("rondas");
        if (llave.getRondas().isEmpty()) {
            rondas.getChildren().add(columna("Sin combates",
                    List.of(tarjetaTexto("Único competidor de la categoría: campeón por W.O.", null))));
        }
        for (Ronda ronda : llave.getRondas()) {
            rondas.getChildren().add(columnaRonda(llave, ronda));
        }

        VBox contenido = new VBox(16, new VBox(4, titulo, subtitulo), rondas);
        if (llave.estaTerminada()) {
            contenido.getChildren().add(podio(llave));
        } else {
            Label pendiente = new Label("Combates pendientes en esta ronda: " + llave.getCombatesPendientes().size()
                    + ". Hacé clic en un combate para cargar el resultado; la siguiente ronda se arma sola "
                    + "cuando terminan todos.");
            pendiente.setWrapText(true);
            pendiente.getStyleClass().add("ayuda");
            contenido.getChildren().add(pendiente);
        }
        contenido.getStyleClass().add("lienzo-llave");
        contenedorLlave.setContent(contenido);
    }

    private static String competidores(int n) {
        return n == 1 ? "1 competidor" : n + " competidores";
    }

    private static String descripcionFormato(Llave llave) {
        return switch (llave.getParticipantes().size()) {
            case 1 -> "Campeón por W.O.";
            case 2 -> "Final directa";
            case 3 -> "Triangular";
            default -> "Eliminación directa por peso";
        };
    }

    private VBox columnaRonda(Llave llave, Ronda ronda) {
        List<Node> tarjetas = new java.util.ArrayList<>();
        for (Combate c : ronda.getCombates()) {
            tarjetas.add(tarjetaCombate(llave, ronda, c));
        }
        if (ronda.getLibre() != null) {
            String texto = ronda.getNombre().startsWith("Triangular") ? "Descansa" : "Pasa libre";
            tarjetas.add(tarjetaTexto(texto, ronda.getLibre()));
        }
        return columna(ronda.getNombre(), tarjetas);
    }

    private static VBox columna(String titulo, List<Node> tarjetas) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("titulo-ronda");
        VBox lista = new VBox(12);
        lista.getChildren().addAll(tarjetas);
        VBox columna = new VBox(10, etiqueta, lista);
        columna.getStyleClass().add("columna-ronda");
        columna.setMinWidth(250);
        columna.setPrefWidth(250);
        return columna;
    }

    private VBox tarjetaCombate(Llave llave, Ronda ronda, Combate c) {
        VBox tarjeta = new VBox(filaCompetidor(c, c.getCompetidor1()), filaCompetidor(c, c.getCompetidor2()));
        tarjeta.getStyleClass().addAll("combate", "combate-clic");
        Resultado r = c.getResultado();
        Label pie = new Label(r == null ? "Pendiente  ·  clic para cargar el resultado" : descripcionResultado(r));
        pie.getStyleClass().add(r == null ? "combate-pendiente" : "combate-resultado");
        tarjeta.getChildren().add(pie);
        javafx.scene.control.Tooltip.install(tarjeta, new javafx.scene.control.Tooltip(
                r == null ? "Cargar resultado" : "Corregir resultado"));
        tarjeta.setOnMouseClicked(e -> {
            if (c.estaTerminado()) {
                corregir(llave, ronda, c);
            } else {
                cargar(ronda, c);
            }
        });
        return tarjeta;
    }

    // ------------------------------------------------------------ resultados

    private void cargar(Ronda ronda, Combate c) {
        DialogoResultado dialogo = new DialogoResultado(c, ronda.getNombre(), raiz.getScene());
        if (dialogo.showAndWait().filter(b -> b == DialogoResultado.GUARDAR).isPresent()) {
            ventana.datosCambiaron();
        }
    }

    /**
     * El resultado nuevo se carga en un combate aparte; recién al guardar se reemplaza
     * en la llave. Si se cancela, no cambia nada.
     */
    private void corregir(Llave llave, Ronda ronda, Combate c) {
        if (!llave.puedeAnular(c)) {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION,
                    "Ya hay resultados cargados en las rondas siguientes. Para corregir este combate, "
                            + "primero corregí (o anulá) esos resultados, empezando por el último.");
            alerta.setHeaderText("No se puede corregir este combate");
            alerta.showAndWait();
            return;
        }
        boolean hayRondaSiguiente = !llave.getRondas().get(llave.getRondas().size() - 1).equals(ronda);
        if (hayRondaSiguiente && !confirmar("Corregir resultado",
                "La ronda siguiente se armó con este resultado. Si cambia el ganador, se vuelve a armar.")) {
            return;
        }
        Combate copia = new Combate(c.getCompetidor1(), c.getCompetidor2());
        DialogoResultado dialogo = new DialogoResultado(copia, ronda.getNombre() + " · corrección", raiz.getScene());
        if (dialogo.showAndWait().filter(b -> b == DialogoResultado.GUARDAR).isPresent()) {
            llave.corregirResultado(c, copia);
            ventana.datosCambiaron();
        }
    }

    /** Ej.: "Sumisión", "Puntos · 4 a 2", "Ventajas · 0 a 0 (V 2-1)". */
    private static String descripcionResultado(Resultado r) {
        if (r.tipo() == TipoVictoria.SUMISION) {
            return "Sumisión";
        }
        Marcador m1 = r.marcador1();
        Marcador m2 = r.marcador2();
        String texto = r.tipo() + "  ·  " + m1.puntos() + " a " + m2.puntos();
        if (m1.ventajas() != 0 || m2.ventajas() != 0) {
            texto += "  (V " + m1.ventajas() + "-" + m2.ventajas() + ")";
        }
        if (m1.penalidades() != 0 || m2.penalidades() != 0) {
            texto += "  (P " + m1.penalidades() + "-" + m2.penalidades() + ")";
        }
        return texto;
    }

    private HBox filaCompetidor(Combate c, Participante p) {
        Label nombre = new Label(p.getNombreCompleto());
        nombre.getStyleClass().add("nombre-competidor");
        nombre.setMaxWidth(160);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label kg = new Label(Formato.kg(p.getPesoOficial()));
        kg.getStyleClass().add("peso-competidor");
        HBox fila = new HBox(6, nombre, espacio, kg);
        if (p.isExcedidoDePeso()) {
            Label aviso = new Label("excedido");
            aviso.getStyleClass().add("etiqueta-excedido");
            fila.getChildren().add(2, aviso);
        }
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("fila-competidor");
        if (c.estaTerminado()) {
            fila.getStyleClass().add(p.equals(c.getGanador()) ? "ganador" : "perdedor");
        }
        if (p.equals(resaltado)) {
            fila.getStyleClass().add("resaltado");
        }
        return fila;
    }

    private Node tarjetaTexto(String texto, Participante p) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("ayuda");
        VBox tarjeta = new VBox(4, etiqueta);
        if (p != null) {
            Label nombre = new Label(p.getNombreCompleto() + "  ·  " + Formato.kg(p.getPesoOficial()));
            nombre.getStyleClass().add("nombre-competidor");
            HBox fila = new HBox(nombre);
            fila.getStyleClass().add("fila-competidor");
            if (p.equals(resaltado)) {
                fila.getStyleClass().add("resaltado");
            }
            tarjeta.getChildren().add(fila);
        }
        tarjeta.getStyleClass().addAll("combate", "combate-libre");
        return tarjeta;
    }

    private Node podio(Llave llave) {
        Label titulo = new Label("Podio");
        titulo.getStyleClass().add("titulo-ronda");
        HBox puestos = new HBox(12);
        for (Puesto puesto : llave.getPodio()) {
            Label lugar = new Label(puesto.lugar() + "°");
            lugar.getStyleClass().addAll("lugar", "lugar-" + puesto.lugar());
            Label nombre = new Label(puesto.participante().getNombreCompleto());
            nombre.getStyleClass().add("nombre-competidor");
            HBox caja = new HBox(8, lugar, nombre);
            caja.setAlignment(Pos.CENTER_LEFT);
            caja.getStyleClass().add("puesto");
            if (puesto.participante().equals(resaltado)) {
                caja.getStyleClass().add("resaltado");
            }
            puestos.getChildren().add(caja);
        }
        return new VBox(8, titulo, puestos);
    }

    private static Node vacio(String titulo, String texto) {
        Label t = new Label(titulo);
        t.getStyleClass().add("titulo-tarjeta");
        Label d = new Label(texto);
        d.getStyleClass().add("ayuda");
        d.setWrapText(true);
        VBox caja = new VBox(8, t, d);
        caja.getStyleClass().add("estado-vacio");
        return caja;
    }

    @Override
    public Node getNodo() {
        return raiz;
    }

    /** Nombre de la categoría, cantidad de competidores y si ya terminó. */
    private final class CeldaCategoria extends ListCell<Categoria> {
        @Override
        protected void updateItem(Categoria c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Llave llave = ventana.getTorneo().getLlaves().get(c);
            Label nombre = new Label(c.getNombre());
            nombre.getStyleClass().add("nombre-categoria");
            String estado = llave.estaTerminada() ? "Terminada" : "En curso";
            Label detalle = new Label(competidores(llave.getParticipantes().size()) + "  ·  " + estado);
            detalle.getStyleClass().add("ayuda");
            setText(null);
            setGraphic(new VBox(2, nombre, detalle));
        }
    }
}
