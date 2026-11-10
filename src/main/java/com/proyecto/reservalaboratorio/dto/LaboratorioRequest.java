package com.proyecto.reservalaboratorio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que recibe la API para crear o actualizar un laboratorio o aula.
 */
public record LaboratorioRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String nombre,

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 150, message = "La ubicación admite hasta 150 caracteres")
    String ubicacion,

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es 1")
    @Max(value = 500, message = "La capacidad máxima es 500")
    Integer capacidad,

    @NotNull(message = "Indica si el laboratorio está activo")
    Boolean activo

) {
}
