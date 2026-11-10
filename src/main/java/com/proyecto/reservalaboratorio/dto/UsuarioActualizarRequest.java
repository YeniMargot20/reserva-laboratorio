package com.proyecto.reservalaboratorio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que recibe la API para actualizar un usuario.
 * La contraseña y el rol no se modifican por esta vía.
 */
public record UsuarioActualizarRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String nombre,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres")
    String correo,

    @NotNull(message = "Indica si el usuario está activo")
    Boolean activo

) {
}
