package com.proyecto.reservalaboratorio.dto;

import com.proyecto.reservalaboratorio.entity.Laboratorio;

/**
 * Datos de un laboratorio que devuelve la API.
 */
public record LaboratorioResponse(
    Long id,
    String nombre,
    String ubicacion,
    Integer capacidad,
    Boolean activo
) {

    public static LaboratorioResponse desde(Laboratorio laboratorio) {
        return new LaboratorioResponse(
            laboratorio.getId(),
            laboratorio.getNombre(),
            laboratorio.getUbicacion(),
            laboratorio.getCapacidad(),
            laboratorio.getActivo()
        );
    }
}
