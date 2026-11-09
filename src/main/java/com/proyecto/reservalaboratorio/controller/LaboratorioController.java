package com.proyecto.reservalaboratorio.controller;

import java.util.List;

import com.proyecto.reservalaboratorio.entity.Laboratorio;
import com.proyecto.reservalaboratorio.service.LaboratorioService;

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

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    public LaboratorioController(
            LaboratorioService laboratorioService) {
        this.laboratorioService = laboratorioService;
    }

    @GetMapping
    public List<Laboratorio> listar() {
        return laboratorioService.listar();
    }

    @GetMapping("/{id}")
    public Laboratorio buscarPorId(@PathVariable("id") Long id) {
        return laboratorioService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Laboratorio crear(
            @Valid @RequestBody Laboratorio laboratorio) {
        return laboratorioService.crear(laboratorio);
    }

    @PutMapping("/{id}")
    public Laboratorio actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Laboratorio laboratorio) {
        return laboratorioService.actualizar(id, laboratorio);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        laboratorioService.eliminar(id);
    }
}
