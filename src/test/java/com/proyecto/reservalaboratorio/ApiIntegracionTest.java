package com.proyecto.reservalaboratorio;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de extremo a extremo de la API REST: recorren
 * Controller -> Service -> Repository -> base de datos (H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    private String tokenAdmin;

    @BeforeEach
    void iniciarSesionAdmin() throws Exception {
        tokenAdmin = login("admin@test.local", "AdminPrueba2026!");
    }

    // ---------- Autenticación ----------

    @Test
    @DisplayName("Login correcto devuelve 200 y un token")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"correo":"admin@test.local","password":"AdminPrueba2026!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Login incorrecto devuelve 401 en JSON")
    void loginIncorrecto() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"correo":"admin@test.local","password":"incorrecta123"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    @DisplayName("Sin token devuelve 401 en JSON")
    void sinToken() throws Exception {
        mvc.perform(get("/api/laboratorios"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
            .andExpect(jsonPath("$.estado").value(401))
            .andExpect(jsonPath("$.ruta").value("/api/laboratorios"));
    }

    @Test
    @DisplayName("Solicitante sin permiso recibe 403 en JSON")
    void solicitanteSinPermiso() throws Exception {
        String token = crearSolicitanteYLogin("sinpermiso@test.local");

        mvc.perform(post("/api/laboratorios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(laboratorioJson("Lab Prohibido", 10)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.estado").value(403));

        mvc.perform(get("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    // ---------- CRUD de laboratorios ----------

    @Test
    @DisplayName("CRUD completo de laboratorio: 201, 200, 200, 204, 404")
    void crudLaboratorio() throws Exception {
        String respuesta = mvc.perform(post("/api/laboratorios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(laboratorioJson("Lab CRUD", 25)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.nombre").value("Lab CRUD"))
            .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(respuesta, "$.id");

        mvc.perform(get("/api/laboratorios/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.capacidad").value(25));

        mvc.perform(get("/api/laboratorios")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].nombre", hasItem("Lab CRUD")));

        mvc.perform(put("/api/laboratorios/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(laboratorioJson("Lab CRUD Editado", 40)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Lab CRUD Editado"))
            .andExpect(jsonPath("$.capacidad").value(40));

        mvc.perform(delete("/api/laboratorios/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/laboratorios/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("Laboratorio no encontrado"));
    }

    @Test
    @DisplayName("Datos inválidos devuelven 400 con el detalle por campo")
    void validacionLaboratorio() throws Exception {
        mvc.perform(post("/api/laboratorios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"","ubicacion":"","capacidad":0,"activo":true}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.nombre").exists())
            .andExpect(jsonPath("$.errores.ubicacion").exists())
            .andExpect(jsonPath("$.errores.capacidad").value("La capacidad mínima es 1"));
    }

    @Test
    @DisplayName("JSON mal formado devuelve 400")
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/api/laboratorios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":"))
            .andExpect(status().isBadRequest());
    }

    // ---------- Usuarios ----------

    @Test
    @DisplayName("Correo duplicado devuelve 409 y la respuesta no expone la contraseña")
    void usuarioDuplicado() throws Exception {
        mvc.perform(post("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(usuarioJson("duplicado@test.local")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rol").value("SOLICITANTE"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(usuarioJson("duplicado@test.local")))
            .andExpect(status().isConflict());
    }

    // ---------- Reservas ----------

    @Test
    @DisplayName("Reservas: crear 201, cruce 409, horario inválido 400, ocupados, borrar 204")
    void reglasDeReserva() throws Exception {
        Integer labId = crearLaboratorio("Lab Reservas");
        Integer usuarioId = crearUsuario("reserva@test.local");
        String fecha = LocalDate.now().plusDays(7).toString();

        String respuesta = mvc.perform(post("/api/reservas")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(usuarioId, labId, fecha, "08:00", "10:00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.laboratorioNombre").value("Lab Reservas"))
            .andReturn().getResponse().getContentAsString();

        Integer reservaId = JsonPath.read(respuesta, "$.id");

        // Se cruza con la reserva de 08:00 a 10:00
        mvc.perform(post("/api/reservas")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(usuarioId, labId, fecha, "09:00", "11:00")))
            .andExpect(status().isConflict());

        // Hora de fin anterior a la de inicio
        mvc.perform(post("/api/reservas")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(usuarioId, labId, fecha, "12:00", "11:00")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.horarioValido").exists());

        // Fecha pasada
        mvc.perform(post("/api/reservas")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(usuarioId, labId,
                    LocalDate.now().minusDays(1).toString(), "08:00", "09:00")))
            .andExpect(status().isBadRequest());

        // Horarios ocupados del laboratorio
        mvc.perform(get("/api/reservas/ocupados")
                .param("laboratorioId", labId.toString())
                .param("fecha", fecha)
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].horaInicio").value("08:00:00"));

        // No se puede borrar un laboratorio con reservas
        mvc.perform(delete("/api/laboratorios/" + labId)
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isConflict());

        mvc.perform(delete("/api/reservas/" + reservaId)
                .header(HttpHeaders.AUTHORIZATION, bearer()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Un solicitante no puede ver reservas ajenas (403)")
    void reservaAjena() throws Exception {
        Integer labId = crearLaboratorio("Lab Privado");
        Integer duenoId = crearUsuario("dueno@test.local");
        String fecha = LocalDate.now().plusDays(3).toString();

        String respuesta = mvc.perform(post("/api/reservas")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservaJson(duenoId, labId, fecha, "14:00", "15:00")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Integer reservaId = JsonPath.read(respuesta, "$.id");
        String otroToken = crearSolicitanteYLogin("otro@test.local");

        mvc.perform(get("/api/reservas/" + reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otroToken))
            .andExpect(status().isForbidden());
    }

    // ---------- Utilidades ----------

    private String bearer() {
        return "Bearer " + tokenAdmin;
    }

    private String login(String correo, String password) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo
                    + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(respuesta, "$.accessToken");
    }

    private Integer crearLaboratorio(String nombre) throws Exception {
        String respuesta = mvc.perform(post("/api/laboratorios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(laboratorioJson(nombre, 20)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(respuesta, "$.id");
    }

    private Integer crearUsuario(String correo) throws Exception {
        String respuesta = mvc.perform(post("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(usuarioJson(correo)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(respuesta, "$.id");
    }

    private String crearSolicitanteYLogin(String correo) throws Exception {
        crearUsuario(correo);
        return login(correo, "ClavePrueba2026");
    }

    private static String laboratorioJson(String nombre, int capacidad) {
        return "{\"nombre\":\"" + nombre + "\",\"ubicacion\":\"Pabellón A\","
            + "\"capacidad\":" + capacidad + ",\"activo\":true}";
    }

    private static String usuarioJson(String correo) {
        return "{\"nombre\":\"Usuario Prueba\",\"correo\":\"" + correo
            + "\",\"password\":\"ClavePrueba2026\"}";
    }

    private static String reservaJson(
            Integer usuarioId, Integer labId, String fecha,
            String inicio, String fin) {

        return "{\"usuarioId\":" + usuarioId + ",\"laboratorioId\":" + labId
            + ",\"fecha\":\"" + fecha + "\",\"horaInicio\":\"" + inicio
            + "\",\"horaFin\":\"" + fin + "\",\"motivo\":\"Clase práctica\","
            + "\"numeroAsistentes\":1}";
    }
}
