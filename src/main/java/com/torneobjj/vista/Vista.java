package com.torneobjj.vista;

import javafx.scene.Node;

/** Una pestaña de la ventana principal. */
interface Vista {

    /** Vuelve a leer los datos del torneo y actualiza la pantalla. */
    void refrescar();

    Node getNodo();
}
