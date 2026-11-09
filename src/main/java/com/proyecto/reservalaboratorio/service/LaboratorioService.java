package com.proyecto.reservalaboratorio.service;

import java.util.List;

import com.proyecto.reservalaboratorio.entity.Laboratorio;
import com.proyecto.reservalaboratorio.repository.LaboratorioRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional
public class LaboratorioService {

    private final LaboratorioRepository laboratorioRepository;

    public LaboratorioService(
            LaboratorioRepository laboratorioRepository) {
        this.laboratorioRepository = laboratorioRepository;
    }

    @Transactional(readOnly = true)
    public List<Laboratorio> listar() {
        return laboratorioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Laboratorio buscarPorId(Long id) {
        return laboratorioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Laboratorio no encontrado"
            ));
    }

    public Laboratorio crear(@Valid Laboratorio laboratorio) {
        laboratorio.setId(null);
        return laboratorioRepository.save(laboratorio);
    }

    public Laboratorio actualizar(
            Long id, @Valid Laboratorio datos) {

        Laboratorio laboratorio = buscarPorId(id);

        laboratorio.setNombre(datos.getNombre());
        laboratorio.setUbicacion(datos.getUbicacion());
        laboratorio.setCapacidad(datos.getCapacidad());
        laboratorio.setActivo(datos.getActivo());

        return laboratorioRepository.save(laboratorio);
    }

    public void eliminar(Long id) {
        Laboratorio laboratorio = buscarPorId(id);
        laboratorioRepository.delete(laboratorio);
    }
}
