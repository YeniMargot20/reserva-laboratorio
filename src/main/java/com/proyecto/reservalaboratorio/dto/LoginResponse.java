package com.proyecto.reservalaboratorio.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    Long usuarioId,
    String nombre,
    String rol
) {
}
