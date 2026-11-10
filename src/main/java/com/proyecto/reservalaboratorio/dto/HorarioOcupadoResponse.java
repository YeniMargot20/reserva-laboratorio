package com.proyecto.reservalaboratorio.dto;

import java.time.LocalTime;

import com.proyecto.reservalaboratorio.entity.Reserva;

/**
 * Franja ocupada de un laboratorio. No incluye datos personales,
 * por eso cualquier usuario puede consultarla antes de reservar.
 */
public record HorarioOcupadoResponse(
    LocalTime horaInicio,
    LocalTime horaFin,
    String estado
) {

    public static HorarioOcupadoResponse desde(Reserva reserva) {
        return new HorarioOcupadoResponse(
            reserva.getHoraInicio(),
            reserva.getHoraFin(),
            reserva.getEstado()
        );
    }
}
