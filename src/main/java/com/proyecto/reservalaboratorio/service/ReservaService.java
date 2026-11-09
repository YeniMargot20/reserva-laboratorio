package com.proyecto.reservalaboratorio.service;

import java.time.LocalDate;
import java.util.List;

import com.proyecto.reservalaboratorio.entity.Laboratorio;
import com.proyecto.reservalaboratorio.entity.Reserva;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.ReservaRepository;
import com.proyecto.reservalaboratorio.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;
    private final UsuarioActualService usuarioActualService;

    public ReservaService(
            ReservaRepository reservaRepository,
            UsuarioRepository usuarioRepository,
            EntityManager entityManager,
            UsuarioActualService usuarioActualService) {

        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listar() {
        Usuario actual = usuarioActualService.obtener();

        if (usuarioActualService.esAdministrador(actual)) {
            return reservaRepository.findAll();
        }

        return reservaRepository.buscarPorUsuario(actual.getId());
    }

    @Transactional(readOnly = true)
    public Reserva buscarPorId(Long id) {
        Usuario actual = usuarioActualService.obtener();

        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reserva no encontrada"
            ));

        comprobarAcceso(actual, reserva);
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<Reserva> buscarPorLaboratorioYFecha(
            Long laboratorioId, LocalDate fecha) {

        Usuario actual = usuarioActualService.obtener();

        if (usuarioActualService.esAdministrador(actual)) {
            return reservaRepository.buscarPorLaboratorioYFecha(
                laboratorioId, fecha
            );
        }

        return reservaRepository.buscarPorLaboratorioFechaYUsuario(
            laboratorioId, fecha, actual.getId()
        );
    }

    public Reserva crear(@NotNull @Valid Reserva datos) {
        Usuario actual = usuarioActualService.obtener();

        if (datos.getLaboratorio() == null
                || datos.getLaboratorio().getId() == null
                || datos.getUsuario() == null
                || datos.getUsuario().getId() == null) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes indicar el ID del laboratorio y del usuario"
            );
        }

        Usuario propietario;

        if (usuarioActualService.esAdministrador(actual)) {
            propietario = usuarioRepository
                .findById(datos.getUsuario().getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
                ));
        } else {
            if (!actual.getId().equals(datos.getUsuario().getId())) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes crear reservas para tu propia cuenta"
                );
            }

            propietario = actual;
        }

        Laboratorio laboratorio = entityManager.find(
            Laboratorio.class,
            datos.getLaboratorio().getId(),
            LockModeType.PESSIMISTIC_WRITE
        );

        if (laboratorio == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Laboratorio no encontrado"
            );
        }

        if (!Boolean.TRUE.equals(laboratorio.getActivo())
                || !Boolean.TRUE.equals(propietario.getActivo())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El laboratorio y el usuario deben estar activos"
            );
        }

        List<Reserva> reservas = reservaRepository
            .buscarPorLaboratorioYFecha(
                laboratorio.getId(), datos.getFecha()
            );

        boolean hayCruce = reservas.stream().anyMatch(reserva ->
            !"CANCELADA".equals(reserva.getEstado())
            && datos.getHoraInicio().isBefore(reserva.getHoraFin())
            && datos.getHoraFin().isAfter(reserva.getHoraInicio())
        );

        if (hayCruce) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El laboratorio ya tiene una reserva en ese horario"
            );
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(propietario);
        reserva.setLaboratorio(laboratorio);
        reserva.setFecha(datos.getFecha());
        reserva.setHoraInicio(datos.getHoraInicio());
        reserva.setHoraFin(datos.getHoraFin());
        reserva.setMotivo(datos.getMotivo());
        reserva.setEstado("PENDIENTE");

        return reservaRepository.save(reserva);
    }

    public Reserva actualizar(Long id, @NotNull @Valid Reserva datos) {
        Usuario actual = usuarioActualService.obtener();

        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reserva no encontrada"
            ));

        comprobarAcceso(actual, reserva);

        Laboratorio laboratorio = entityManager.find(
            Laboratorio.class,
            reserva.getLaboratorio().getId(),
            LockModeType.PESSIMISTIC_WRITE
        );

        entityManager.refresh(reserva);
        comprobarAcceso(actual, reserva);

        if (datos.getUsuario() == null
                || !reserva.getUsuario().getId()
                    .equals(datos.getUsuario().getId())
                || datos.getLaboratorio() == null
                || !laboratorio.getId()
                    .equals(datos.getLaboratorio().getId())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede cambiar el usuario ni el laboratorio de la reserva"
            );
        }

        if (!Boolean.TRUE.equals(laboratorio.getActivo())
                || !Boolean.TRUE.equals(reserva.getUsuario().getActivo())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El laboratorio y el usuario deben estar activos"
            );
        }

        boolean esAdministrador =
            usuarioActualService.esAdministrador(actual);

        if (!esAdministrador
                && !reserva.getEstado().equals(datos.getEstado())) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el administrador puede cambiar el estado"
            );
        }

        String estado = esAdministrador
            ? datos.getEstado()
            : reserva.getEstado();

        if (!"CANCELADA".equals(estado)) {
            boolean hayCruce = reservaRepository
                .buscarPorLaboratorioYFecha(
                    laboratorio.getId(), datos.getFecha()
                )
                .stream()
                .anyMatch(otra ->
                    !otra.getId().equals(reserva.getId())
                    && !"CANCELADA".equals(otra.getEstado())
                    && datos.getHoraInicio().isBefore(otra.getHoraFin())
                    && datos.getHoraFin().isAfter(otra.getHoraInicio())
                );

            if (hayCruce) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El laboratorio ya tiene una reserva en ese horario"
                );
            }
        }

        reserva.setFecha(datos.getFecha());
        reserva.setHoraInicio(datos.getHoraInicio());
        reserva.setHoraFin(datos.getHoraFin());
        reserva.setMotivo(datos.getMotivo());
        reserva.setEstado(estado);

        return reservaRepository.save(reserva);
    }

    public void eliminar(Long id) {
        Usuario actual = usuarioActualService.obtener();

        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reserva no encontrada"
            ));

        comprobarAcceso(actual, reserva);
        reservaRepository.delete(reserva);
    }

    private void comprobarAcceso(Usuario actual, Reserva reserva) {
        boolean esPropietario = reserva.getUsuario() != null
            && actual.getId().equals(reserva.getUsuario().getId());

        if (!usuarioActualService.esAdministrador(actual)
                && !esPropietario) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para acceder a esta reserva"
            );
        }
    }
}