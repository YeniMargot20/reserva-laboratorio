package com.proyecto.reservalaboratorio.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorRespuesta(
    Instant fecha,
    int estado,
    String mensaje,
    String ruta,
    Map<String, String> errores
) {
}
