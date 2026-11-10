package com.proyecto.reservalaboratorio.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.proyecto.reservalaboratorio.entity.Reserva;

/**
 * Datos de una reserva que devuelve la API.
 */
public record ReservaResponse(
    Long id,
    Long usuarioId,
    String usuarioNombre,
    Long laboratorioId,
    String laboratorioNombre,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String motivo,
    String estado
) {

    public static ReservaResponse desde(Reserva reserva) {
        return new ReservaResponse(
            reserva.getId(),
            reserva.getUsuario().getId(),
            reserva.getUsuario().getNombre(),
            reserva.getLaboratorio().getId(),
            reserva.getLaboratorio().getNombre(),
            reserva.getFecha(),
            reserva.getHoraInicio(),
            reserva.getHoraFin(),
            reserva.getMotivo(),
            reserva.getEstado()
        );
    }
}
