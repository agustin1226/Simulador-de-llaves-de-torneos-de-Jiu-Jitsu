package com.torneobjj.vista;

import com.torneobjj.modelo.CategoriaPeso;
import com.torneobjj.modelo.DivisionEdad;
import com.torneobjj.modelo.Nivel;
import com.torneobjj.modelo.Participante;
import com.torneobjj.modelo.Sexo;
import com.torneobjj.servicio.Torneo;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Formulario de inscripción y listado de inscriptos. */
class VistaInscripcion implements Vista {

    private final VentanaPrincipal ventana;
    private final BorderPane raiz = new BorderPane();

    private final TextField dni = new TextField();
    private final TextField nombre = new TextField();
    private final TextField apellido = new TextField();
    private final DatePicker nacimiento = new DatePicker();
    private final Label infoEdad = new Label();
    private final ToggleGroup sexo = new ToggleGroup();
    private final ToggleGroup nivel = new ToggleGroup();
    private final ComboBox<CategoriaPeso> peso = new ComboBox<>();
    private final Mensaje mensaje = new Mensaje();
    private final Button inscribir = new Button("Inscribir");
    private final Label avisoSorteo = new Label("Las llaves ya fueron sorteadas: no se puede inscribir. "
            + "Para hacerlo, anulá el sorteo desde la pestaña Llaves.");

    private final TableView<Participante> tabla = Tablas.nueva("Todavía no hay inscriptos");
    private final Button eliminar = new Button("Eliminar seleccionado");

    VistaInscripcion(VentanaPrincipal ventana) {
        this.ventana = ventana;
        raiz.getStyleClass().add("pagina");
        raiz.setLeft(formulario());
        raiz.setCenter(listado());
        BorderPane.setMargin(raiz.getCenter(), new Insets(0, 0, 0, 20));
    }

    // ------------------------------------------------------------ formulario

    private Node formulario() {
        Label titulo = new Label("Nuevo competidor");
        titulo.getStyleClass().add("titulo-tarjeta");

        dni.setPromptText("Sin puntos");
        nacimiento.setPromptText("dd/mm/aaaa");
        nacimiento.setConverter(new ConvertidorFecha());
        nacimiento.setMaxWidth(Double.MAX_VALUE);
        nacimiento.valueProperty().addListener((o, a, b) -> actualizarInfoEdad());
        nacimiento.getEditor().textProperty().addListener((o, a, b) -> actualizarInfoEdad());
        infoEdad.getStyleClass().add("ayuda");

        HBox sexos = opciones(sexo, Sexo.values());
        HBox niveles = opciones(nivel, Nivel.values());
        sexo.selectedToggleProperty().addListener((o, a, b) -> actualizarCategoriasPeso());

        peso.setPromptText("Elegí primero el sexo");
        peso.setDisable(true);
        peso.setMaxWidth(Double.MAX_VALUE);
        peso.setCellFactory(lv -> new CeldaPeso());
        peso.setButtonCell(new CeldaPeso());

        GridPane grilla = new GridPane();
        grilla.getStyleClass().add("grilla-formulario");
        int fila = 0;
        grilla.addRow(fila++, etiqueta("DNI"), dni);
        grilla.addRow(fila++, etiqueta("Nombre"), nombre);
        grilla.addRow(fila++, etiqueta("Apellido"), apellido);
        grilla.addRow(fila++, etiqueta("Nacimiento"), nacimiento);
        grilla.add(infoEdad, 1, fila++);
        grilla.addRow(fila++, etiqueta("Sexo"), sexos);
        grilla.addRow(fila++, etiqueta("Nivel"), niveles);
        grilla.addRow(fila++, etiqueta("Peso"), peso);
        GridPane.setHgrow(dni, Priority.ALWAYS);

        inscribir.setDefaultButton(true);
        inscribir.getStyleClass().add("boton-primario");
        inscribir.setOnAction(e -> inscribir());
        Button limpiar = new Button("Limpiar");
        limpiar.setOnAction(e -> {
            limpiar();
            mensaje.ocultar();
        });
        HBox botones = new HBox(8, limpiar, inscribir);
        botones.setAlignment(Pos.CENTER_RIGHT);

        avisoSorteo.setWrapText(true);
        avisoSorteo.getStyleClass().addAll("mensaje", "mensaje-advertencia");

        VBox tarjeta = new VBox(14, titulo, avisoSorteo, grilla, botones, mensaje);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPrefWidth(460);
        tarjeta.setMinWidth(440);
        actualizarInfoEdad();
        return tarjeta;
    }

    private <T> HBox opciones(ToggleGroup grupo, T[] valores) {
        HBox caja = new HBox(12);
        for (T valor : valores) {
            RadioButton r = new RadioButton(valor.toString());
            r.setUserData(valor);
            r.setToggleGroup(grupo);
            caja.getChildren().add(r);
        }
        return caja;
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private Sexo sexoElegido() {
        return valorElegido(sexo, Sexo.class);
    }

    private static <T> T valorElegido(ToggleGroup grupo, Class<T> tipo) {
        Toggle t = grupo.getSelectedToggle();
        return t == null ? null : tipo.cast(t.getUserData());
    }

    /** Al cambiar el sexo cambian las categorías (las mujeres no tienen Pesadísimo y los límites son otros). */
    private void actualizarCategoriasPeso() {
        Sexo s = sexoElegido();
        CategoriaPeso anterior = peso.getValue();
        if (s == null) {
            peso.getItems().clear();
            peso.setDisable(true);
            return;
        }
        List<CategoriaPeso> disponibles = CategoriaPeso.disponiblesPara(s);
        peso.setItems(FXCollections.observableArrayList(disponibles));
        peso.setDisable(false);
        peso.setPromptText("Elegí la categoría");
        peso.setValue(disponibles.contains(anterior) ? anterior : null);
        peso.setButtonCell(new CeldaPeso()); // para que se actualice el texto del límite
    }

    private void actualizarInfoEdad() {
        infoEdad.getStyleClass().remove("texto-error");
        Optional<LocalDate> fecha = leerFecha();
        if (fecha.isEmpty()) {
            infoEdad.setText("La edad y la división se calculan solas");
            return;
        }
        int anio = ventana.getTorneo().getAnio();
        int edad = anio - fecha.get().getYear();
        Optional<DivisionEdad> division = DivisionEdad.desdeEdad(edad);
        if (fecha.get().isAfter(ventana.getTorneo().getFecha())) {
            infoEdad.setText("La fecha es posterior al torneo");
            infoEdad.getStyleClass().add("texto-error");
        } else if (division.isEmpty()) {
            infoEdad.setText(edad + " años en " + anio + ": menor de " + DivisionEdad.EDAD_MINIMA
                    + ", no puede competir");
            infoEdad.getStyleClass().add("texto-error");
        } else {
            infoEdad.setText(edad + " años en " + anio + "  →  " + division.get().getDescripcion());
        }
    }

    /** Lee la fecha del texto escrito aunque todavía no se haya confirmado con Enter. */
    private Optional<LocalDate> leerFecha() {
        String texto = nacimiento.getEditor().getText();
        if (texto == null || texto.isBlank()) {
            return Optional.ofNullable(nacimiento.getValue());
        }
        try {
            return Optional.of(LocalDate.parse(texto.trim(), Formato.FECHA));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private void inscribir() {
        Torneo torneo = ventana.getTorneo();
        try {
            LocalDate fecha = leerFecha().orElseThrow(() ->
                    new IllegalArgumentException("Ingresá una fecha de nacimiento válida (dd/mm/aaaa)"));
            Sexo s = sexoElegido();
            Nivel n = valorElegido(nivel, Nivel.class);
            if (s == null) {
                throw new IllegalArgumentException("Elegí el sexo");
            }
            if (n == null) {
                throw new IllegalArgumentException("Elegí el nivel");
            }
            if (peso.getValue() == null) {
                throw new IllegalArgumentException("Elegí la categoría de peso");
            }
            Participante p = new Participante(dni.getText(), nombre.getText(), apellido.getText(),
                    fecha, s, n, peso.getValue());
            torneo.inscribir(p);
            limpiar();
            mensaje.exito(p.getNombreCompleto() + " inscripto en " + p.getCategoria(torneo.getAnio()));
            ventana.datosCambiaron();
            tabla.getSelectionModel().select(p);
            tabla.scrollTo(p);
            dni.requestFocus();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensaje.error(ex.getMessage());
        }
    }

    private void limpiar() {
        dni.clear();
        nombre.clear();
        apellido.clear();
        nacimiento.setValue(null);
        nacimiento.getEditor().clear();
        sexo.selectToggle(null);
        nivel.selectToggle(null);
        peso.setValue(null);
    }

    // ------------------------------------------------------------ listado

    private Node listado() {
        tabla.getColumns().addAll(List.of(
                Tablas.<Participante, String>columna("DNI", Participante::getDni, 90),
                Tablas.<Participante, String>columna("Apellido y nombre",
                        p -> p.getApellido() + ", " + p.getNombre(), 180),
                Tablas.<Participante, Integer>columna("Edad", p -> p.getEdadEnAnio(anio()), 55),
                Tablas.<Participante, String>columna("División",
                        p -> p.getCategoria(anio()).division().toString(), 80),
                Tablas.<Participante, Sexo>columna("Sexo", Participante::getSexo, 85),
                Tablas.<Participante, Nivel>columna("Nivel", Participante::getNivel, 95),
                Tablas.<Participante, String>columna("Categoría de peso",
                        p -> p.getCategoriaPeso().getDescripcion(p.getSexo()), 190)));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        eliminar.setOnAction(e -> {
            Participante p = tabla.getSelectionModel().getSelectedItem();
            if (p != null) {
                ventana.getTorneo().eliminar(p);
                mensaje.exito(p.getNombreCompleto() + " fue eliminado");
                ventana.datosCambiaron();
            }
        });
        eliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());

        Label titulo = new Label("Inscriptos");
        titulo.getStyleClass().add("titulo-tarjeta");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(8, titulo, espacio, eliminar);
        barra.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(12, barra, tabla);
        caja.getStyleClass().add("tarjeta");
        return caja;
    }

    private int anio() {
        return ventana.getTorneo().getAnio();
    }

    @Override
    public void refrescar() {
        actualizarInfoEdad();
        boolean sorteado = ventana.getTorneo().estaSorteado();
        avisoSorteo.setVisible(sorteado);
        avisoSorteo.setManaged(sorteado);
        inscribir.setDisable(sorteado);
        eliminar.setVisible(!sorteado);
        Participante seleccionado = tabla.getSelectionModel().getSelectedItem();
        tabla.setItems(FXCollections.observableArrayList(ventana.getTorneo().getParticipantes()));
        if (seleccionado != null) {
            tabla.getSelectionModel().select(seleccionado);
        }
        tabla.refresh();
    }

    @Override
    public Node getNodo() {
        return raiz;
    }

    // ------------------------------------------------------------ clases auxiliares

    /** Muestra "Medio pesado (hasta 88,3 kg)" según el sexo elegido. */
    private final class CeldaPeso extends ListCell<CategoriaPeso> {
        @Override
        protected void updateItem(CategoriaPeso item, boolean empty) {
            super.updateItem(item, empty);
            Sexo s = sexoElegido();
            if (empty || item == null) {
                setText(peso.getPromptText());
            } else {
                setText(s == null ? item.toString() : item.getDescripcion(s));
            }
        }
    }

    private static final class ConvertidorFecha extends StringConverter<LocalDate> {
        @Override
        public String toString(LocalDate fecha) {
            return Formato.fecha(fecha);
        }

        @Override
        public LocalDate fromString(String texto) {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(texto.trim(), Formato.FECHA);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}
