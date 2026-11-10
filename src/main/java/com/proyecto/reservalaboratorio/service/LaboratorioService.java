package com.proyecto.reservalaboratorio.service;

import java.util.List;

import com.proyecto.reservalaboratorio.dto.LaboratorioRequest;
import com.proyecto.reservalaboratorio.dto.LaboratorioResponse;
import com.proyecto.reservalaboratorio.entity.Laboratorio;
import com.proyecto.reservalaboratorio.repository.LaboratorioRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
    public List<LaboratorioResponse> listar() {
        return laboratorioRepository.findAll()
            .stream()
            .map(LaboratorioResponse::desde)
            .toList();
    }

    @Transactional(readOnly = true)
    public LaboratorioResponse buscarPorId(Long id) {
        return LaboratorioResponse.desde(obtener(id));
    }

    public LaboratorioResponse crear(
            @NotNull @Valid LaboratorioRequest datos) {

        Laboratorio laboratorio = new Laboratorio();
        copiarDatos(datos, laboratorio);

        return LaboratorioResponse.desde(
            laboratorioRepository.save(laboratorio)
        );
    }

    public LaboratorioResponse actualizar(
            Long id, @NotNull @Valid LaboratorioRequest datos) {

        Laboratorio laboratorio = obtener(id);
        copiarDatos(datos, laboratorio);

        return LaboratorioResponse.desde(
            laboratorioRepository.save(laboratorio)
        );
    }

    public void eliminar(Long id) {
        Laboratorio laboratorio = obtener(id);
        laboratorioRepository.delete(laboratorio);
        // Escribe el cambio ahora para que un laboratorio con
        // reservas se informe como conflicto (409).
        laboratorioRepository.flush();
    }

    private Laboratorio obtener(Long id) {
        return laboratorioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Laboratorio no encontrado"
            ));
    }

    private void copiarDatos(
            LaboratorioRequest datos, Laboratorio laboratorio) {

        laboratorio.setNombre(datos.nombre().trim());
        laboratorio.setUbicacion(datos.ubicacion().trim());
        laboratorio.setCapacidad(datos.capacidad());
        laboratorio.setActivo(datos.activo());
    }
}
