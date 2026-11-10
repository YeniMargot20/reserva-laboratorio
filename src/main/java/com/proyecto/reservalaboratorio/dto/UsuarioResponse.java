package com.proyecto.reservalaboratorio.dto;

import com.proyecto.reservalaboratorio.entity.Usuario;

/**
 * Datos de un usuario que devuelve la API (nunca incluye la contraseña).
 */
public record UsuarioResponse(
    Long id,
    String nombre,
    String correo,
    Boolean activo,
    String rol
) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getActivo(),
            usuario.getRol() != null ? usuario.getRol().name() : null
        );
    }
}
