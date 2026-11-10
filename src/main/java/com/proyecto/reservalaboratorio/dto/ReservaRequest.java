package com.proyecto.reservalaboratorio.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que recibe la API para crear o actualizar una reserva.
 * El estado es opcional: solo el administrador puede cambiarlo.
 */
public record ReservaRequest(

    @NotNull(message = "El usuario es obligatorio")
    Long usuarioId,

    @NotNull(message = "El laboratorio es obligatorio")
    Long laboratorioId,

    @NotNull(message = "La fecha es obligatoria")
    LocalDate fecha,

    @NotNull(message = "La hora de inicio es obligatoria")
    LocalTime horaInicio,

    @NotNull(message = "La hora de fin es obligatoria")
    LocalTime horaFin,

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 200, message = "El motivo admite hasta 200 caracteres")
    String motivo,

    @Pattern(
        regexp = "PENDIENTE|CONFIRMADA|CANCELADA",
        message = "El estado debe ser PENDIENTE, CONFIRMADA o CANCELADA"
    )
    String estado

) {

    @JsonIgnore
    @AssertTrue(message = "La hora de fin debe ser posterior a la de inicio")
    public boolean isHorarioValido() {
        return horaInicio == null
            || horaFin == null
            || horaFin.isAfter(horaInicio);
    }
}
