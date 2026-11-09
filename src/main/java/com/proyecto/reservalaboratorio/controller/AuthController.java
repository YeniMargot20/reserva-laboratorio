package com.proyecto.reservalaboratorio.controller;

import com.proyecto.reservalaboratorio.dto.LoginRequest;
import com.proyecto.reservalaboratorio.dto.LoginResponse;
import com.proyecto.reservalaboratorio.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest datos) {

        LoginResponse respuesta = authService.login(datos);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("Pragma", "no-cache")
            .body(respuesta);
    }
}
