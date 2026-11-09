package com.proyecto.reservalaboratorio.controller;

import java.time.LocalDate;
import java.util.List;

import com.proyecto.reservalaboratorio.entity.Reserva;
import com.proyecto.reservalaboratorio.service.ReservaService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<Reserva> listar() {
        return reservaService.listar();
    }

    @GetMapping("/{id}")
    public Reserva buscarPorId(@PathVariable("id") Long id) {
        return reservaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reserva crear(@Valid @RequestBody Reserva reserva) {
        return reservaService.crear(reserva);
    }

    @PutMapping("/{id}")
    public Reserva actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Reserva reserva) {

        return reservaService.actualizar(id, reserva);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        reservaService.eliminar(id);
    }

    @GetMapping("/buscar")
    public List<Reserva> buscarPorLaboratorioYFecha(
            @RequestParam("laboratorioId") Long laboratorioId,
            @RequestParam("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return reservaService.buscarPorLaboratorioYFecha(
            laboratorioId, fecha
        );
    }
}