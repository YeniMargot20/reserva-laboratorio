package com.proyecto.reservalaboratorio.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import com.proyecto.reservalaboratorio.dto.UsuarioCrearRequest;
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

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));
    }

    public Usuario crear(@NotNull @Valid UsuarioCrearRequest datos) {
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

        return usuarioRepository.save(usuario);
    }

    public Usuario actualizar(Long id, @NotNull @Valid Usuario datos) {
        Usuario usuario = buscarPorId(id);
        String correo = normalizarCorreo(datos.getCorreo());

        comprobarCorreoDisponible(correo, id);

        usuario.setNombre(datos.getNombre().trim());
        usuario.setCorreo(correo);
        usuario.setActivo(datos.getActivo());

        return usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.buscarPorCorreo(
                normalizarCorreo(correo)
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));
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