package com.torneobjj.vista;

import com.torneobjj.modelo.Combate;
import com.torneobjj.modelo.Marcador;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.ReglaDesempate;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Carga el resultado de un combate: por sumisión (se elige el ganador) o por puntos
 * (puntos, ventajas y penalidades de cada uno). Con empate total aplica la regla del
 * exceso de peso o, si no alcanza, pide la decisión del árbitro.
 */
class DialogoResultado extends Dialog<ButtonType> {

    static final ButtonType GUARDAR = new ButtonType("Guardar resultado", ButtonBar.ButtonData.OK_DONE);

    private final Combate combate;
    private final Participante p1;
    private final Participante p2;

    private final ToggleGroup forma = new ToggleGroup();
    private final RadioButton porSumision = new RadioButton("Sumisión");
    private final RadioButton porPuntos = new RadioButton("Puntos");

    private final ToggleGroup ganadorSumision = new ToggleGroup();
    private final VBox panelSumision;

    private final Spinner<Integer> puntos1 = contador();
    private final Spinner<Integer> puntos2 = contador();
    private final Spinner<Integer> ventajas1 = contador();
    private final Spinner<Integer> ventajas2 = contador();
    private final Spinner<Integer> penalidades1 = contador();
    private final Spinner<Integer> penalidades2 = contador();
    private final VBox panelPuntos;

    private final ToggleGroup eleccionArbitro = new ToggleGroup();
    private final VBox panelArbitro;

    private final Label vistaPrevia = new Label();
    private final Mensaje error = new Mensaje();

    DialogoResultado(Combate combate, String nombreRonda, Scene escenaPadre) {
        this.combate = combate;
        this.p1 = combate.getCompetidor1();
        this.p2 = combate.getCompetidor2();

        setTitle("Cargar resultado");
        setHeaderText(null);
        if (escenaPadre != null) {
            initOwner(escenaPadre.getWindow());
            getDialogPane().getStylesheets().addAll(escenaPadre.getStylesheets());
        }
        getDialogPane().getStyleClass().add("dialogo-resultado");

        Label titulo = new Label(nombreRonda);
        titulo.getStyleClass().add("titulo-ronda");
        Label cruce = new Label(p1.getNombreCompleto() + "  vs  " + p2.getNombreCompleto());
        cruce.getStyleClass().add("titulo-tarjeta");
        cruce.setWrapText(true);

        porSumision.setToggleGroup(forma);
        porPuntos.setToggleGroup(forma);
        Label etiquetaForma = new Label("¿Cómo terminó?");
        etiquetaForma.getStyleClass().add("etiqueta");
        HBox formas = new HBox(18, etiquetaForma, porSumision, porPuntos);
        formas.setAlignment(Pos.CENTER_LEFT);

        panelSumision = armarSumision();
        panelPuntos = armarPuntos();
        panelArbitro = armarArbitro();

        vistaPrevia.getStyleClass().add("vista-previa");
        vistaPrevia.setWrapText(true);
        vistaPrevia.setMaxWidth(Double.MAX_VALUE);

        VBox contenido = new VBox(16, new VBox(2, titulo, cruce), formas,
                panelSumision, panelPuntos, panelArbitro, vistaPrevia, error);
        contenido.setPrefWidth(560);
        getDialogPane().setContent(contenido);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, GUARDAR);

        // Validar antes de cerrar: si falta algo, el diálogo queda abierto
        Button guardar = (Button) getDialogPane().lookupButton(GUARDAR);
        guardar.addEventFilter(ActionEvent.ACTION, e -> {
            if (!guardar()) {
                e.consume();
            }
        });

        forma.selectedToggleProperty().addListener((o, a, b) -> actualizar());
        ganadorSumision.selectedToggleProperty().addListener((o, a, b) -> actualizar());
        eleccionArbitro.selectedToggleProperty().addListener((o, a, b) -> actualizar());
        for (Spinner<Integer> s : new Spinner[] {puntos1, puntos2, ventajas1, ventajas2, penalidades1, penalidades2}) {
            s.valueProperty().addListener((o, a, b) -> actualizar());
        }
        porPuntos.setSelected(true);
        actualizar();
    }

    // ------------------------------------------------------------ armado

    private VBox armarSumision() {
        Label etiqueta = new Label("¿Quién ganó por sumisión?");
        etiqueta.getStyleClass().add("etiqueta");
        return new VBox(8, etiqueta, opcionCompetidor(ganadorSumision, p1), opcionCompetidor(ganadorSumision, p2));
    }

    private VBox armarPuntos() {
        GridPane grilla = new GridPane();
        grilla.getStyleClass().add("grilla-marcador");
        ColumnConstraints nombre = new ColumnConstraints();
        nombre.setHgrow(Priority.ALWAYS);
        ColumnConstraints numero = new ColumnConstraints(90);
        numero.setHalignment(HPos.CENTER);
        grilla.getColumnConstraints().addAll(nombre, numero, numero, numero);

        grilla.addRow(0, new Label(), encabezado("Puntos"), encabezado("Ventajas"), encabezado("Penalidades"));
        grilla.addRow(1, nombreCompetidor(p1), puntos1, ventajas1, penalidades1);
        grilla.addRow(2, nombreCompetidor(p2), puntos2, ventajas2, penalidades2);

        Label ayuda = new Label("Desempate: puntos → ventajas → menos penalidades → pierde el excedido de peso "
                + "→ decisión del árbitro.");
        ayuda.getStyleClass().add("ayuda");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return new VBox(10, grilla, ayuda);
    }

    private VBox armarArbitro() {
        Label etiqueta = new Label("Empate total: ¿a quién le da la victoria el árbitro?");
        etiqueta.getStyleClass().add("etiqueta");
        VBox caja = new VBox(8, etiqueta, opcionCompetidor(eleccionArbitro, p1), opcionCompetidor(eleccionArbitro, p2));
        caja.getStyleClass().add("panel-arbitro");
        return caja;
    }

    private static RadioButton opcionCompetidor(ToggleGroup grupo, Participante p) {
        RadioButton r = new RadioButton(p.getNombreCompleto() + "  (" + Formato.kg(p.getPesoOficial())
                + (p.isExcedidoDePeso() ? ", excedido" : "") + ")");
        r.setUserData(p);
        r.setToggleGroup(grupo);
        return r;
    }

    private static Node nombreCompetidor(Participante p) {
        Label nombre = new Label(p.getNombreCompleto());
        nombre.getStyleClass().add("nombre-competidor");
        HBox caja = new HBox(6, nombre);
        caja.setAlignment(Pos.CENTER_LEFT);
        if (p.isExcedidoDePeso()) {
            Label aviso = new Label("excedido");
            aviso.getStyleClass().add("etiqueta-excedido");
            caja.getChildren().add(aviso);
        }
        return caja;
    }

    private static Label encabezado(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta");
        return l;
    }

    private static Spinner<Integer> contador() {
        Spinner<Integer> s = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 99, 0));
        s.setEditable(true);
        s.setPrefWidth(80);
        // Si se escribe un número y se cambia de campo, que se tome el valor escrito
        s.focusedProperty().addListener((o, antes, ahora) -> {
            if (!ahora) {
                confirmarTexto(s);
            }
        });
        return s;
    }

    private static void confirmarTexto(Spinner<Integer> s) {
        try {
            int valor = Integer.parseInt(s.getEditor().getText().trim());
            s.getValueFactory().setValue(Math.max(0, Math.min(99, valor)));
        } catch (NumberFormatException e) {
            s.getEditor().setText(String.valueOf(s.getValue()));
        }
    }

    // ------------------------------------------------------------ lógica

    private Marcador marcador1() {
        return new Marcador(puntos1.getValue(), ventajas1.getValue(), penalidades1.getValue());
    }

    private Marcador marcador2() {
        return new Marcador(puntos2.getValue(), ventajas2.getValue(), penalidades2.getValue());
    }

    /** Muestra solo el panel que corresponde y explica quién ganaría con lo cargado. */
    private void actualizar() {
        error.ocultar();
        boolean sumision = porSumision.isSelected();
        mostrar(panelSumision, sumision);
        mostrar(panelPuntos, !sumision);

        if (sumision) {
            mostrar(panelArbitro, false);
            Participante g = elegido(ganadorSumision);
            vistaPrevia.setText(g == null ? "Elegí al ganador." : "Gana " + g.getNombreCompleto() + " por sumisión.");
        } else {
            Optional<ReglaDesempate.Decision> decision = ReglaDesempate.decidir(p1, marcador1(), p2, marcador2());
            mostrar(panelArbitro, decision.isEmpty());
            if (decision.isPresent()) {
                vistaPrevia.setText(textoDecision(decision.get()));
            } else {
                Participante g = elegido(eleccionArbitro);
                vistaPrevia.setText(g == null
                        ? "Empate total y ninguno (o los dos) excedido de peso: decide el árbitro."
                        : "Gana " + g.getNombreCompleto() + " por decisión del árbitro.");
            }
        }
        if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
            getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    private String textoDecision(ReglaDesempate.Decision d) {
        Participante perdedor = d.ganador().equals(p1) ? p2 : p1;
        String motivo = switch (d.tipo()) {
            case PUNTOS -> "por puntos";
            case VENTAJAS -> "por ventajas (empate en puntos)";
            case PENALIDADES -> "por tener menos penalidades (empate en puntos y ventajas)";
            case PESO -> "por empate total: " + perdedor.getNombreCompleto() + " se pasó de peso";
            default -> "";
        };
        return "Gana " + d.ganador().getNombreCompleto() + " " + motivo + ".";
    }

    private static Participante elegido(ToggleGroup grupo) {
        return grupo.getSelectedToggle() == null ? null : (Participante) grupo.getSelectedToggle().getUserData();
    }

    private static void mostrar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    /** Registra el resultado en el combate. Devuelve false (y muestra el error) si falta algo. */
    boolean guardar() {
        try {
            if (porSumision.isSelected()) {
                Participante g = elegido(ganadorSumision);
                if (g == null) {
                    throw new IllegalArgumentException("Elegí quién ganó por sumisión");
                }
                combate.registrarSumision(g);
                return true;
            }
            for (Spinner<Integer> s : new Spinner[] {puntos1, puntos2, ventajas1, ventajas2, penalidades1, penalidades2}) {
                confirmarTexto(s);
            }
            Marcador m1 = marcador1();
            Marcador m2 = marcador2();
            if (combate.requiereDecisionArbitro(m1, m2)) {
                Participante g = elegido(eleccionArbitro);
                if (g == null) {
                    throw new IllegalArgumentException("Hay empate total: elegí a quién le da la victoria el árbitro");
                }
                combate.registrarDecisionArbitro(m1, m2, g);
            } else {
                combate.registrarPorPuntos(m1, m2);
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error.error(ex.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------ para pruebas

    void elegirSumision(Participante ganador) {
        porSumision.setSelected(true);
        ganadorSumision.getToggles().stream()
                .filter(t -> t.getUserData().equals(ganador)).findFirst()
                .ifPresent(t -> t.setSelected(true));
    }

    void cargarPuntos(Marcador m1, Marcador m2) {
        porPuntos.setSelected(true);
        puntos1.getValueFactory().setValue(m1.puntos());
        ventajas1.getValueFactory().setValue(m1.ventajas());
        penalidades1.getValueFactory().setValue(m1.penalidades());
        puntos2.getValueFactory().setValue(m2.puntos());
        ventajas2.getValueFactory().setValue(m2.ventajas());
        penalidades2.getValueFactory().setValue(m2.penalidades());
    }

    void elegirArbitro(Participante ganador) {
        eleccionArbitro.getToggles().stream()
                .filter(t -> t.getUserData().equals(ganador)).findFirst()
                .ifPresent(t -> t.setSelected(true));
    }

    String getVistaPrevia() {
        return vistaPrevia.getText();
    }

    String getError() {
        return error.isVisible() ? error.getText() : "";
    }
}
