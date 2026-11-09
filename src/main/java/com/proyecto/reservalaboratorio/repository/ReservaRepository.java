package com.proyecto.reservalaboratorio.repository;

import java.time.LocalDate;
import java.util.List;

import com.proyecto.reservalaboratorio.entity.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.laboratorio.id = :laboratorioId
          AND r.fecha = :fecha
        ORDER BY r.horaInicio
        """)
    List<Reserva> buscarPorLaboratorioYFecha(
        @Param("laboratorioId") Long laboratorioId,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.usuario.id = :usuarioId
        ORDER BY r.fecha DESC, r.horaInicio DESC, r.id DESC
        """)
    List<Reserva> buscarPorUsuario(
        @Param("usuarioId") Long usuarioId
    );

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.laboratorio.id = :laboratorioId
          AND r.fecha = :fecha
          AND r.usuario.id = :usuarioId
        ORDER BY r.horaInicio
        """)
    List<Reserva> buscarPorLaboratorioFechaYUsuario(
        @Param("laboratorioId") Long laboratorioId,
        @Param("fecha") LocalDate fecha,
        @Param("usuarioId") Long usuarioId
    );
}