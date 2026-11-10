package com.proyecto.reservalaboratorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.proyecto.reservalaboratorio.dto.ReservaRequest;
import com.proyecto.reservalaboratorio.dto.ReservaResponse;
import com.proyecto.reservalaboratorio.entity.Laboratorio;
import com.proyecto.reservalaboratorio.entity.Reserva;
import com.proyecto.reservalaboratorio.entity.Rol;
import com.proyecto.reservalaboratorio.entity.Usuario;
import com.proyecto.reservalaboratorio.repository.LaboratorioRepository;
import com.proyecto.reservalaboratorio.repository.ReservaRepository;
import com.proyecto.reservalaboratorio.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pruebas unitarias de las reglas de negocio de ReservaService.
 * Los repositorios se simulan con Mockito, sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LaboratorioRepository laboratorioRepository;
    @Mock private EntityManager entityManager;
    @Mock private UsuarioActualService usuarioActualService;

    @InjectMocks private ReservaService reservaService;

    private Usuario solicitante;
    private Laboratorio laboratorio;
    private final LocalDate manana = LocalDate.now().plusDays(1);

    @BeforeEach
    void preparar() {
        solicitante = new Usuario();
        solicitante.setId(10L);
        solicitante.setNombre("Ana");
        solicitante.setActivo(true);
        solicitante.setRol(Rol.SOLICITANTE);

        laboratorio = new Laboratorio();
        laboratorio.setId(1L);
        laboratorio.setNombre("Lab 1");
        laboratorio.setActivo(true);
        laboratorio.setCapacidad(30);
    }

    @Test
    @DisplayName("Crea la reserva en estado PENDIENTE si el horario está libre")
    void creaReservaLibre() {
        when(usuarioActualService.obtener()).thenReturn(solicitante);
        when(entityManager.find(Laboratorio.class, 1L, LockModeType.PESSIMISTIC_WRITE))
            .thenReturn(laboratorio);
        when(reservaRepository.buscarPorLaboratorioYFecha(1L, manana))
            .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class)))
            .thenAnswer(invocacion -> {
                Reserva r = invocacion.getArgument(0);
                r.setId(99L);
                return r;
            });

        ReservaResponse respuesta = reservaService.crear(
            solicitud(10L, "08:00", "10:00")
        );

        assertThat(respuesta.id()).isEqualTo(99L);
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.usuarioId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Rechaza con 409 una reserva que se cruza con otra")
    void rechazaCruce() {
        when(usuarioActualService.obtener()).thenReturn(solicitante);
        when(entityManager.find(Laboratorio.class, 1L, LockModeType.PESSIMISTIC_WRITE))
            .thenReturn(laboratorio);
        when(reservaRepository.buscarPorLaboratorioYFecha(eq(1L), eq(manana)))
            .thenReturn(List.of(reservaExistente("09:00", "11:00", "CONFIRMADA")));

        assertThatThrownBy(() ->
            reservaService.crear(solicitud(10L, "10:00", "12:00")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una reserva CANCELADA no bloquea el horario")
    void canceladaNoBloquea() {
        when(usuarioActualService.obtener()).thenReturn(solicitante);
        when(entityManager.find(Laboratorio.class, 1L, LockModeType.PESSIMISTIC_WRITE))
            .thenReturn(laboratorio);
        when(reservaRepository.buscarPorLaboratorioYFecha(1L, manana))
            .thenReturn(List.of(reservaExistente("08:00", "10:00", "CANCELADA")));
        when(reservaRepository.save(any(Reserva.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaResponse respuesta = reservaService.crear(
            solicitud(10L, "08:00", "10:00")
        );

        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Un solicitante no puede reservar a nombre de otro usuario (403)")
    void noReservaParaOtro() {
        when(usuarioActualService.obtener()).thenReturn(solicitante);
        when(usuarioActualService.esAdministrador(solicitante)).thenReturn(false);

        assertThatThrownBy(() ->
            reservaService.crear(solicitud(20L, "08:00", "10:00")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("No permite reservar en una fecha pasada (400)")
    void noFechaPasada() {
        when(usuarioActualService.obtener()).thenReturn(solicitante);

        ReservaRequest ayer = new ReservaRequest(
            10L, 1L, LocalDate.now().minusDays(1),
            LocalTime.of(8, 0), LocalTime.of(9, 0), "Clase", null, 1
        );

        assertThatThrownBy(() -> reservaService.crear(ayer))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

       @Test
       @DisplayName("Rechaza con 400 si los asistentes superan la capacidad")
       void rechazaExcesoDeAforo() {
           when(usuarioActualService.obtener()).thenReturn(solicitante);
           when(entityManager.find(Laboratorio.class, 1L, LockModeType.PESSIMISTIC_WRITE))
               .thenReturn(laboratorio);

           // El laboratorio de la prueba tiene capacidad 30
           ReservaRequest exceso = new ReservaRequest(
               10L, 1L, manana,
               LocalTime.of(8, 0), LocalTime.of(10, 0),
               "Clase", null, 31
           );

           assertThatThrownBy(() -> reservaService.crear(exceso))
               .isInstanceOf(ResponseStatusException.class)
               .extracting(e -> ((ResponseStatusException) e).getStatusCode())
               .isEqualTo(HttpStatus.BAD_REQUEST);

           verify(reservaRepository, never()).save(any());
       }

    private ReservaRequest solicitud(Long usuarioId, String inicio, String fin) {
        return new ReservaRequest(
            usuarioId, 1L, manana,
            LocalTime.parse(inicio), LocalTime.parse(fin),
            "Clase práctica", null, 1
        );
    }

    private Reserva reservaExistente(String inicio, String fin, String estado) {
        Reserva reserva = new Reserva();
        reserva.setId(5L);
        reserva.setUsuario(solicitante);
        reserva.setLaboratorio(laboratorio);
        reserva.setFecha(manana);
        reserva.setHoraInicio(LocalTime.parse(inicio));
        reserva.setHoraFin(LocalTime.parse(fin));
        reserva.setEstado(estado);
        return reserva;
    }
}
