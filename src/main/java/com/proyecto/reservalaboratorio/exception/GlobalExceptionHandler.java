package com.proyecto.reservalaboratorio.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.proyecto.reservalaboratorio.dto.ErrorRespuesta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            errores.putIfAbsent(
                error.getField(),
                error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "Valor inválido"
            )
        );

        ex.getBindingResult().getGlobalErrors().forEach(error ->
            errores.putIfAbsent(
                error.getObjectName(),
                error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "Datos inválidos"
            )
        );

        return responder(
            400, "Revisa los datos ingresados", request, errores
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarRestricciones(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(error ->
            errores.putIfAbsent(
                error.getPropertyPath().toString(),
                error.getMessage()
            )
        );

        return responder(
            400, "Revisa los datos ingresados", request, errores
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorRespuesta> manejarEstado(
            ResponseStatusException ex,
            HttpServletRequest request) {

        ErrorRespuesta respuesta = new ErrorRespuesta(
            Instant.now(),
            ex.getStatusCode().value(),
            ex.getReason() != null
                ? ex.getReason()
                : "No se pudo completar la solicitud",
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.status(ex.getStatusCode())
            .headers(ex.getHeaders())
            .body(respuesta);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarConflicto(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return responder(
            409,
            "La operación genera un conflicto: revisa los datos "
                + "duplicados o los registros relacionados",
            request,
            Map.of()
        );
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorRespuesta> manejarFormato(
            Exception ex,
            HttpServletRequest request) {

        return responder(
            400,
            "El cuerpo o los parámetros tienen un formato inválido",
            request,
            Map.of()
        );
    }

    private ResponseEntity<ErrorRespuesta> responder(
            int estado,
            String mensaje,
            HttpServletRequest request,
            Map<String, String> errores) {

        ErrorRespuesta respuesta = new ErrorRespuesta(
            Instant.now(),
            estado,
            mensaje,
            request.getRequestURI(),
            errores
        );

        return ResponseEntity.status(HttpStatus.valueOf(estado))
            .body(respuesta);
    }
}
