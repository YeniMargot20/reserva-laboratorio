package com.proyecto.reservalaboratorio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres")
    String correo,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 64, message = "La contraseña admite hasta 64 caracteres")
    String password

) {
}
