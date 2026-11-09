package com.proyecto.reservalaboratorio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCrearRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String nombre,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres")
    String correo,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
        min = 12,
        max = 64,
        message = "La contraseña debe tener entre 12 y 64 caracteres"
    )
    String password

) {
}