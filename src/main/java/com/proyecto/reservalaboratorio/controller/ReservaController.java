package com.proyecto.reservalaboratorio.controller;

import java.time.LocalDate;
import java.util.List;

import com.proyecto.reservalaboratorio.dto.HorarioOcupadoResponse;
import com.proyecto.reservalaboratorio.dto.IndicadorOcupacionResponse;
import com.proyecto.reservalaboratorio.dto.ReservaRequest;
import com.proyecto.reservalaboratorio.dto.ReservaResponse;
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
    public List<ReservaResponse> listar() {
        return reservaService.listar();
    }

    @GetMapping("/{id}")
    public ReservaResponse buscarPorId(@PathVariable("id") Long id) {
        return reservaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest datos) {
        return reservaService.crear(datos);
    }

    @PutMapping("/{id}")
    public ReservaResponse actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReservaRequest datos) {

        return reservaService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        reservaService.eliminar(id);
    }

    @GetMapping("/buscar")
    public List<ReservaResponse> buscarPorLaboratorioYFecha(
            @RequestParam("laboratorioId") Long laboratorioId,
            @RequestParam("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return reservaService.buscarPorLaboratorioYFecha(
            laboratorioId, fecha
        );
    }

    @GetMapping("/ocupados")
    public List<HorarioOcupadoResponse> horariosOcupados(
            @RequestParam("laboratorioId") Long laboratorioId,
            @RequestParam("fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha)
            
            {

        return reservaService.horariosOcupados(laboratorioId, fecha);
    }

        @GetMapping("/indicadores")
    public List<IndicadorOcupacionResponse> indicadores(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        return reservaService.indicadores(desde, hasta);
    }
}
