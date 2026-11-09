package com.proyecto.reservalaboratorio.controller;

import java.util.List;

import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.proyecto.reservalaboratorio.dto.UsuarioCrearRequest;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable("id") Long id) {
        return usuarioService.buscarPorId(id);
    }

   @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario crear(
    @Valid @RequestBody UsuarioCrearRequest datos
    ) {
    return usuarioService.crear(datos);
    }

    @PutMapping("/{id}")
    public Usuario actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Usuario usuario) {
        return usuarioService.actualizar(id, usuario);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        usuarioService.eliminar(id);
    }
        @GetMapping("/buscar")
    public Usuario buscarPorCorreo(
            @RequestParam("correo") String correo) {
        return usuarioService.buscarPorCorreo(correo);
    }
}
