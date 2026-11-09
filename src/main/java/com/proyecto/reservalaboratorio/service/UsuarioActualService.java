package com.proyecto.reservalaboratorio.service;

import com.proyecto.reservalaboratorio.entity.Rol;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioActualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualService(
            UsuarioRepository usuarioRepository) {

        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Usuario obtener() {

        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt)) {

            throw noAutorizado();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();

        Long usuarioId;

        try {
            usuarioId = Long.valueOf(jwt.getSubject());
        }
        catch (NumberFormatException ex) {
            throw noAutorizado();
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> noAutorizado());

        if (!Boolean.TRUE.equals(usuario.getActivo())
                || usuario.getRol() == null) {

            throw noAutorizado();
        }

        if (!usuario.getRol().name().equals(
                jwt.getClaimAsString("rol"))) {

            throw noAutorizado();
        }

        return usuario;
    }

    public boolean esAdministrador(Usuario usuario) {
        return usuario.getRol() == Rol.ADMINISTRADOR;
    }

    private ResponseStatusException noAutorizado() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Debes iniciar sesión con una cuenta activa"
        );
    }
}