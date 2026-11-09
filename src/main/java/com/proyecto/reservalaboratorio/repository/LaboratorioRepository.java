package com.proyecto.reservalaboratorio.repository;

import com.proyecto.reservalaboratorio.entity.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratorioRepository
        extends JpaRepository<Laboratorio, Long> {
}
