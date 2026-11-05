package com.torneobjj.llave;

import com.torneobjj.modelo.Participante;

/** Lugar en el podio (1, 2 o 3; puede haber dos terceros). */
public record Puesto(int lugar, Participante participante) {
}
