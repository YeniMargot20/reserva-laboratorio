package com.proyecto.reservalaboratorio.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import com.proyecto.reservalaboratorio.dto.UsuarioActualizarRequest;
import com.proyecto.reservalaboratorio.dto.UsuarioCrearRequest;
import com.proyecto.reservalaboratorio.dto.UsuarioResponse;
import com.proyecto.reservalaboratorio.entity.Rol;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.UsuarioRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActualService usuarioActualService;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        UsuarioActualService usuarioActualService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll()
            .stream()
            .map(UsuarioResponse::desde)
            .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.desde(obtener(id));
    }

    public UsuarioResponse crear(@NotNull @Valid UsuarioCrearRequest datos) {

        String correo = normalizarCorreo(datos.correo());
        comprobarCorreoDisponible(correo, null);

        if (datos.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contraseña no debe superar 72 bytes en UTF-8"
            );
        }

        Usuario usuario = new Usuario();

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setActivo(true);
        usuario.setRol(Rol.SOLICITANTE);
        usuario.setPasswordHash(
            passwordEncoder.encode(datos.password())
        );

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    public UsuarioResponse actualizar(
            Long id, @NotNull @Valid UsuarioActualizarRequest datos) {

        Usuario usuario = obtener(id);

        String correo = normalizarCorreo(datos.correo());
        comprobarCorreoDisponible(correo, id);

        if (!datos.activo() && esUsuarioActual(id)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No puedes desactivar tu propia cuenta"
            );
        }

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setActivo(datos.activo());

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    public void eliminar(Long id) {

        Usuario usuario = obtener(id);

        if (esUsuarioActual(id)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No puedes eliminar tu propia cuenta"
            );
        }

        usuarioRepository.delete(usuario);
        // Un usuario con reservas se informa como conflicto (409).
        usuarioRepository.flush();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorCorreo(String correo) {
        return usuarioRepository.buscarPorCorreo(
                normalizarCorreo(correo)
            )
            .map(UsuarioResponse::desde)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));
    }

    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));
    }

    private boolean esUsuarioActual(Long id) {
        return usuarioActualService.obtener().getId().equals(id);
    }

    private String normalizarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El correo es obligatorio"
            );
        }

        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void comprobarCorreoDisponible(
        String correo,
        Long usuarioIdActual
    ) {
        usuarioRepository.buscarPorCorreo(correo)
            .filter(usuario ->
                !usuario.getId().equals(usuarioIdActual)
            )
            .ifPresent(usuario -> {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
                );
            });
    }
}
