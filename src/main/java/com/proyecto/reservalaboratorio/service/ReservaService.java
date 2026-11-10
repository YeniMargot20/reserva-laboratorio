package com.proyecto.reservalaboratorio.service;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.proyecto.reservalaboratorio.dto.HorarioOcupadoResponse;
import com.proyecto.reservalaboratorio.dto.IndicadorOcupacionResponse;
import com.proyecto.reservalaboratorio.dto.ReservaRequest;
import com.proyecto.reservalaboratorio.dto.ReservaResponse;
import com.proyecto.reservalaboratorio.entity.Laboratorio;
import com.proyecto.reservalaboratorio.entity.Reserva;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.LaboratorioRepository;
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

    // Horario de atención del laboratorio, usado para calcular la ocupación.
    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(22, 0);

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final EntityManager entityManager;
    private final UsuarioActualService usuarioActualService;

    public ReservaService(
            ReservaRepository reservaRepository,
            UsuarioRepository usuarioRepository,
            LaboratorioRepository laboratorioRepository,
            EntityManager entityManager,
            UsuarioActualService usuarioActualService) {

        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.entityManager = entityManager;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar() {
        Usuario actual = usuarioActualService.obtener();

        List<Reserva> reservas =
            usuarioActualService.esAdministrador(actual)
                ? reservaRepository.findAll()
                : reservaRepository.buscarPorUsuario(actual.getId());

        return reservas.stream().map(ReservaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse buscarPorId(Long id) {
        Usuario actual = usuarioActualService.obtener();
        Reserva reserva = obtener(id);

        comprobarAcceso(actual, reserva);

        return ReservaResponse.desde(reserva);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> buscarPorLaboratorioYFecha(
            Long laboratorioId, LocalDate fecha) {

        Usuario actual = usuarioActualService.obtener();

        List<Reserva> reservas =
            usuarioActualService.esAdministrador(actual)
                ? reservaRepository.buscarPorLaboratorioYFecha(
                    laboratorioId, fecha
                )
                : reservaRepository.buscarPorLaboratorioFechaYUsuario(
                    laboratorioId, fecha, actual.getId()
                );

        return reservas.stream().map(ReservaResponse::desde).toList();
    }

    /**
     * Franjas ya ocupadas de un laboratorio en una fecha. Permite al
     * solicitante elegir un horario libre antes de enviar la reserva.
     */
    @Transactional(readOnly = true)
    public List<HorarioOcupadoResponse> horariosOcupados(
            Long laboratorioId, LocalDate fecha) {

        usuarioActualService.obtener();

        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Laboratorio no encontrado"
            );
        }

        return reservaRepository
            .buscarPorLaboratorioYFecha(laboratorioId, fecha)
            .stream()
            .filter(reserva -> !"CANCELADA".equals(reserva.getEstado()))
            .map(HorarioOcupadoResponse::desde)
            .toList();
    }

    public ReservaResponse crear(@NotNull @Valid ReservaRequest datos) {

        Usuario actual = usuarioActualService.obtener();

        if (datos.fecha().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se puede reservar en una fecha pasada"
            );
        }

        Usuario propietario;

        if (usuarioActualService.esAdministrador(actual)) {
            propietario = usuarioRepository
                .findById(datos.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado"
                ));
        } else {
            if (!actual.getId().equals(datos.usuarioId())) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes crear reservas para tu propia cuenta"
                );
            }

            propietario = actual;
        }

        // Bloquea el laboratorio para evitar dos reservas simultáneas
        // en el mismo horario.
        Laboratorio laboratorio = entityManager.find(
            Laboratorio.class,
            datos.laboratorioId(),
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
        comprobarAforo(laboratorio, datos);
        comprobarCruce(laboratorio.getId(), datos, null);

        Reserva reserva = new Reserva();

        reserva.setUsuario(propietario);
        reserva.setLaboratorio(laboratorio);
        reserva.setFecha(datos.fecha());
        reserva.setHoraInicio(datos.horaInicio());
        reserva.setHoraFin(datos.horaFin());
        reserva.setMotivo(datos.motivo().trim());
                reserva.setNumeroAsistentes(datos.numeroAsistentes());
        reserva.setEstado("PENDIENTE");

        return ReservaResponse.desde(reservaRepository.save(reserva));
    }

    public ReservaResponse actualizar(
            Long id, @NotNull @Valid ReservaRequest datos) {

        Usuario actual = usuarioActualService.obtener();

        Reserva reserva = obtener(id);
        comprobarAcceso(actual, reserva);

        Laboratorio laboratorio = entityManager.find(
            Laboratorio.class,
            reserva.getLaboratorio().getId(),
            LockModeType.PESSIMISTIC_WRITE
        );

        entityManager.refresh(reserva);
        comprobarAcceso(actual, reserva);

        if (!reserva.getUsuario().getId().equals(datos.usuarioId())
                || !laboratorio.getId().equals(datos.laboratorioId())) {
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
        comprobarAforo(laboratorio, datos);
        boolean esAdministrador =
            usuarioActualService.esAdministrador(actual);

        if (!esAdministrador
                && datos.estado() != null
                && !reserva.getEstado().equals(datos.estado())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el administrador puede cambiar el estado"
            );
        }

        String estado = esAdministrador && datos.estado() != null
            ? datos.estado()
            : reserva.getEstado();

        if (!"CANCELADA".equals(estado)) {
            comprobarCruce(laboratorio.getId(), datos, reserva.getId());
        }

        reserva.setFecha(datos.fecha());
        reserva.setHoraInicio(datos.horaInicio());
        reserva.setHoraFin(datos.horaFin());
        reserva.setMotivo(datos.motivo().trim());
        reserva.setNumeroAsistentes(datos.numeroAsistentes());
        reserva.setEstado(estado);

        return ReservaResponse.desde(reservaRepository.save(reserva));
    }

    public void eliminar(Long id) {

        Usuario actual = usuarioActualService.obtener();

        Reserva reserva = obtener(id);
        comprobarAcceso(actual, reserva);

        reservaRepository.delete(reserva);
    }

    /**
     * Rechaza la reserva si hay más asistentes que la capacidad
     * del laboratorio.
     */
    private void comprobarAforo(Laboratorio laboratorio, ReservaRequest datos) {

        if (datos.numeroAsistentes() > laboratorio.getCapacidad()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El número de asistentes (" + datos.numeroAsistentes()
                    + ") supera la capacidad del laboratorio ("
                    + laboratorio.getCapacidad() + ")"
            );
        }
    }

    /**
     * Indicadores de ocupación por laboratorio en un rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<IndicadorOcupacionResponse> indicadores(
            LocalDate desde, LocalDate hasta) {

        if (hasta.isBefore(desde)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha final no puede ser anterior a la inicial"
            );
        }

        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        double horasPorDia =
            Duration.between(HORA_APERTURA, HORA_CIERRE).toMinutes() / 60.0;
        double horasDisponibles = dias * horasPorDia;

        Map<Long, List<Reserva>> reservasPorLaboratorio = reservaRepository
            .buscarActivasEntreFechas(desde, hasta)
            .stream()
            .collect(Collectors.groupingBy(
                reserva -> reserva.getLaboratorio().getId()
            ));

        return laboratorioRepository.findAll().stream()
            .filter(lab -> Boolean.TRUE.equals(lab.getActivo()))
            .map(lab -> {
                List<Reserva> reservas = reservasPorLaboratorio
                    .getOrDefault(lab.getId(), List.of());

                double horasReservadas = reservas.stream()
                    .mapToLong(r -> Duration
                        .between(r.getHoraInicio(), r.getHoraFin())
                        .toMinutes())
                    .sum() / 60.0;

                double promedioAsistentes = reservas.stream()
                    .mapToInt(Reserva::getNumeroAsistentes)
                    .average()
                    .orElse(0);

                return new IndicadorOcupacionResponse(
                    lab.getId(),
                    lab.getNombre(),
                    lab.getCapacidad(),
                    reservas.size(),
                    redondear(horasReservadas),
                    redondear(horasDisponibles),
                    redondear(horasReservadas / horasDisponibles * 100),
                    redondear(promedioAsistentes),
                    redondear(promedioAsistentes / lab.getCapacidad() * 100)
                );
            })
            .toList();
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private Reserva obtener(Long id) {
        return reservaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reserva no encontrada"
            ));
    }

    /**
     * Rechaza la reserva si se cruza con otra no cancelada del mismo
     * laboratorio y fecha. Se ignora la propia reserva al actualizar.
     */
    private void comprobarCruce(
            Long laboratorioId, ReservaRequest datos, Long reservaIdActual) {

        boolean hayCruce = reservaRepository
            .buscarPorLaboratorioYFecha(laboratorioId, datos.fecha())
            .stream()
            .anyMatch(otra ->
                !otra.getId().equals(reservaIdActual)
                && !"CANCELADA".equals(otra.getEstado())
                && datos.horaInicio().isBefore(otra.getHoraFin())
                && datos.horaFin().isAfter(otra.getHoraInicio())
            );

        if (hayCruce) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El laboratorio ya tiene una reserva en ese horario"
            );
        }
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
