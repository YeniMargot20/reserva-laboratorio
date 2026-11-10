package com.proyecto.reservalaboratorio.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import com.proyecto.reservalaboratorio.dto.ErrorRespuesta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Devuelve los errores 401 y 403 de Spring Security con el mismo
 * formato JSON (ErrorRespuesta) que el resto de errores de la API.
 */
@Component
public class ErroresSeguridadHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ErroresSeguridadHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex) throws IOException {

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        escribir(
            request,
            response,
            HttpStatus.UNAUTHORIZED,
            "Debes iniciar sesión o tu sesión ha expirado"
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex) throws IOException {

        escribir(
            request,
            response,
            HttpStatus.FORBIDDEN,
            "No tienes permiso para realizar esta acción"
        );
    }

    private void escribir(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus estado,
            String mensaje) throws IOException {

        ErrorRespuesta error = new ErrorRespuesta(
            Instant.now(),
            estado.value(),
            mensaje,
            request.getRequestURI(),
            Map.of()
        );

        response.setStatus(estado.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
