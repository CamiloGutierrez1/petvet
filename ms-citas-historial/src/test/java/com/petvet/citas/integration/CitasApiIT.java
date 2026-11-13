package com.petvet.citas.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petvet.citas.domain.DependenciaNoDisponibleException;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import jakarta.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Timeout(30)
class CitasApiIT {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void configurarDependencias(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("petvet.eventos.habilitados", () -> false);
        registry.add("petvet.vacunas.habilitadas", () -> false);
    }

    @LocalServerPort
    int puerto;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @MockitoBean
    MascotaClient mascotas;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private LocalDate fecha;

    @BeforeEach
    void prepararEscenario() {
        jdbc.execute("TRUNCATE TABLE outbox_eventos, registros_historial, citas RESTART IDENTITY CASCADE");
        fecha = LocalDate.now(BOGOTA).plusDays(4);
        when(mascotas.buscar(anyLong())).thenAnswer(invocation -> new Mascota(
                invocation.getArgument(0, Long.class), "Mascota Demo", Especie.PERRO,
                LocalDate.now(BOGOTA).minusMonths(6), "Dueño Demo", "demo@example.test"));
    }

    @Test
    void listaLosTresVeterinariosYLaDisponibilidadDeUnDia() throws Exception {
        var veterinarios = consultar("GET", "/api/veterinarios", null);
        assertThat(veterinarios.estado()).isEqualTo(200);
        assertThat(veterinarios.cuerpo().size()).isEqualTo(3);
        assertThat(veterinarios.cuerpo().get(0).path("id").asLong()).isEqualTo(1);

        var disponibilidad = consultar("GET", "/api/veterinarios/1/disponibilidad?fecha=" + fecha, null);
        assertThat(disponibilidad.estado()).isEqualTo(200);
        assertThat(disponibilidad.cuerpo().path("veterinarioId").asLong()).isEqualTo(1);
        assertThat(disponibilidad.cuerpo().path("fecha").asText()).isEqualTo(fecha.toString());
        assertThat(disponibilidad.cuerpo().path("horarios").size()).isEqualTo(20);
        assertThat(LocalDateTime.parse(disponibilidad.cuerpo().path("horarios").get(0).asText()))
                .isEqualTo(fecha.atTime(8, 0));
    }

    @Test
    void agendarPersisteLaCitaYUnSoloEventoEnLaMismaTransaccion() throws Exception {
        var creada = agendar(3, 1, fecha.atTime(9, 0));
        assertThat(creada.estado()).isEqualTo(201);
        long id = creada.cuerpo().path("id").asLong();
        assertThat(id).isPositive();
        assertThat(creada.cuerpo().path("estado").asText()).isEqualTo("AGENDADA");
        LocalDateTime fechaGuardada = jdbc.queryForObject("SELECT fecha_hora FROM citas WHERE id = ?",
                (rs, rowNum) -> rs.getObject("fecha_hora", LocalDateTime.class), id);
        assertThat(fechaGuardada).isEqualTo(fecha.atTime(9, 0));

        var porMascota = consultar("GET", "/api/citas?mascotaId=3", null);
        var agenda = consultar("GET", "/api/citas?veterinarioId=1&fecha=" + fecha, null);
        assertThat(porMascota.estado()).isEqualTo(200);
        assertThat(porMascota.cuerpo().get(0).path("id").asLong()).isEqualTo(id);
        assertThat(agenda.cuerpo().get(0).path("id").asLong()).isEqualTo(id);
        assertThat(contarEventos("cita.agendada:" + id)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT payload ->> 'duenoEmail' FROM outbox_eventos WHERE clave_deduplicacion = ?",
                String.class, "cita.agendada:" + id)).isEqualTo("demo@example.test");
    }

    @Test
    void rechazaHorarioOcupadoYSuDisponibilidadSeRecuperaAlCancelar() throws Exception {
        var creada = agendar(3, 1, fecha.atTime(10, 0));
        assertThat(creada.estado()).isEqualTo(201);
        long id = creada.cuerpo().path("id").asLong();
        assertError(agendar(4, 1, fecha.atTime(10, 0)), 409);

        var ocupada = consultar("GET", "/api/veterinarios/1/disponibilidad?fecha=" + fecha, null);
        assertThat(ocupada.cuerpo().path("horarios").size()).isEqualTo(19);

        var cancelada = consultar("PATCH", "/api/citas/" + id + "/cancelar", null);
        assertThat(cancelada.estado()).isEqualTo(200);
        assertThat(cancelada.cuerpo().path("estado").asText()).isEqualTo("CANCELADA");
        assertThat(consultar("PATCH", "/api/citas/" + id + "/cancelar", null).estado()).isEqualTo(200);
        assertThat(agendar(4, 1, fecha.atTime(10, 0)).estado()).isEqualTo(201);
    }

    @Test
    void consultaUnaCitaPorSuIdConSuEstadoActual() throws Exception {
        long id = agendar(3, 2, fecha.atTime(11, 0)).cuerpo().path("id").asLong();

        var cita = consultar("GET", "/api/citas/" + id, null);
        assertThat(cita.estado()).isEqualTo(200);
        assertThat(cita.cuerpo().path("id").asLong()).isEqualTo(id);
        assertThat(cita.cuerpo().path("mascotaId").asLong()).isEqualTo(3);
        assertThat(cita.cuerpo().path("veterinarioId").asLong()).isEqualTo(2);
        assertThat(LocalDateTime.parse(cita.cuerpo().path("fechaHora").asText())).isEqualTo(fecha.atTime(11, 0));
        assertThat(cita.cuerpo().path("tipoServicio").asText()).isEqualTo("CONSULTA_GENERAL");
        assertThat(cita.cuerpo().path("estado").asText()).isEqualTo("AGENDADA");

        consultar("PATCH", "/api/citas/" + id + "/cancelar", null);
        assertThat(consultar("GET", "/api/citas/" + id, null).cuerpo().path("estado").asText())
                .isEqualTo("CANCELADA");
    }

    @Test
    void sinFechaListaTodasLasCitasDelVeterinarioDeLaMasRecienteALaMasAntigua() throws Exception {
        long primera = agendar(3, 1, fecha.atTime(9, 0)).cuerpo().path("id").asLong();
        long otroDia = agendar(4, 1, fecha.plusDays(1).atTime(8, 30)).cuerpo().path("id").asLong();
        long cancelada = agendar(5, 1, fecha.atTime(15, 0)).cuerpo().path("id").asLong();
        consultar("PATCH", "/api/citas/" + cancelada + "/cancelar", null);
        agendar(3, 2, fecha.atTime(10, 0));

        var citas = consultar("GET", "/api/citas?veterinarioId=1", null);
        assertThat(citas.estado()).isEqualTo(200);
        assertThat(citas.cuerpo().findValuesAsText("id").stream().map(Long::parseLong).toList())
                .containsExactly(otroDia, cancelada, primera);
        assertThat(citas.cuerpo().findValuesAsText("veterinarioId")).containsOnly("1");

        var agendaDelDia = consultar("GET", "/api/citas?veterinarioId=1&fecha=" + fecha, null);
        assertThat(agendaDelDia.cuerpo().findValuesAsText("id").stream().map(Long::parseLong).toList())
                .containsExactly(primera, cancelada);
    }

    @Test
    void unaMascotaNoPuedeTenerDosVeterinariosEnElMismoHorario() throws Exception {
        assertThat(agendar(3, 1, fecha.atTime(10, 30)).estado()).isEqualTo(201);
        assertError(agendar(3, 2, fecha.atTime(10, 30)), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas", Long.class)).isEqualTo(1);
    }

    @Test
    void citasConDuracionGuardadaBloqueanTodosSusTurnosSolapados() throws Exception {
        jdbc.update("""
                INSERT INTO citas (mascota_id, veterinario_id, fecha_hora, tipo_servicio, estado, duracion_minutos)
                VALUES (3, 1, ?, 'CONSULTA_GENERAL', 'AGENDADA', 60)
                """, fecha.atTime(9, 0));

        var disponibilidad = consultar("GET", "/api/veterinarios/1/disponibilidad?fecha=" + fecha, null);
        assertThat(disponibilidad.estado()).isEqualTo(200);
        assertThat(disponibilidad.cuerpo().path("horarios").size()).isEqualTo(18);
        for (JsonNode horario : disponibilidad.cuerpo().path("horarios")) {
            assertThat(LocalDateTime.parse(horario.asText()))
                    .isNotEqualTo(fecha.atTime(9, 0))
                    .isNotEqualTo(fecha.atTime(9, 30));
        }

        var cruceVeterinario = agendar(4, 1, fecha.atTime(9, 30));
        assertError(cruceVeterinario, 409);
        assertThat(cruceVeterinario.cuerpo().path("detalle").asText()).contains("veterinario");
        var cruceMascota = agendar(3, 2, fecha.atTime(9, 30));
        assertError(cruceMascota, 409);
        assertThat(cruceMascota.cuerpo().path("detalle").asText()).contains("mascota");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos", Long.class)).isZero();
        assertThat(agendar(4, 1, fecha.atTime(8, 30)).estado()).isEqualTo(201);
        assertThat(agendar(4, 1, fecha.atTime(10, 0)).estado()).isEqualTo(201);
    }

    @Test
    void elHistorialSeLeeConUnNumeroFijoDeConsultasSinImportarCuantosRegistrosTenga() throws Exception {
        for (int i = 0; i < 6; i++) {
            long id = agendar(3, 1, fecha.atTime(8 + i, 0)).cuerpo().path("id").asLong();
            var consulta = consultar("POST", "/api/citas/" + id + "/consulta", Map.of(
                    "tipoConsulta", "GENERAL", "diagnostico", "Consulta " + i,
                    "vacunasAplicadas", List.of("Vacuna " + i),
                    "tratamientos", List.of("Tratamiento " + i, "Reposo " + i),
                    "examenes", List.of("Examen " + i)));
            assertThat(consulta.estado()).isEqualTo(201);
        }
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            var historial = consultar("GET", "/api/mascotas/3/historial", null);

            assertThat(historial.estado()).isEqualTo(200);
            assertThat(historial.cuerpo().size()).isEqualTo(6);
            // Una consulta para los registros y una por cada lista. Antes eran 1 + 3 por registro.
            assertThat(estadisticas.getPrepareStatementCount()).isLessThanOrEqualTo(4);
            for (JsonNode registro : historial.cuerpo()) {
                String numero = registro.path("diagnostico").asText().substring("Consulta ".length());
                assertThat(registro.path("vacunasAplicadas")).extracting(JsonNode::asText)
                        .containsExactly("Vacuna " + numero);
                assertThat(registro.path("tratamientos")).extracting(JsonNode::asText)
                        .containsExactly("Tratamiento " + numero, "Reposo " + numero);
                assertThat(registro.path("examenes")).extracting(JsonNode::asText)
                        .containsExactly("Examen " + numero);
            }
        } finally {
            estadisticas.setStatisticsEnabled(false);
        }
    }

    @Test
    void dosRegistrosSimultaneosDeLaMismaConsultaSoloCompletanLaCitaUnaVez() throws Exception {
        long id = agendar(3, 1, fecha.atTime(15, 0)).cuerpo().path("id").asLong();
        var iniciar = new CountDownLatch(1);
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primera = ejecutor.submit(() -> {
                iniciar.await();
                return consultar("POST", "/api/citas/" + id + "/consulta", datosConsulta());
            });
            var segunda = ejecutor.submit(() -> {
                iniciar.await();
                return consultar("POST", "/api/citas/" + id + "/consulta", datosConsulta());
            });
            iniciar.countDown();
            var resultados = List.of(primera.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
            assertThat(resultados).extracting(Respuesta::estado).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, id))
                .isEqualTo("COMPLETADA");
        assertThat(contarEventos("cita.completada:" + id)).isEqualTo(1);
    }

    @Test
    void registrarConsultaCompletaLaCitaYConservaElHistorial() throws Exception {
        long id = agendar(3, 1, fecha.atTime(11, 0)).cuerpo().path("id").asLong();
        var consulta = consultar("POST", "/api/citas/" + id + "/consulta", datosConsulta());
        assertThat(consulta.estado()).isEqualTo(201);
        assertThat(consulta.cuerpo().path("diagnostico").asText()).isEqualTo("Consulta de demostración");
        assertThat(consulta.cuerpo().path("tipoConsulta").asText()).isEqualTo("GENERAL");
        assertThat(consulta.cuerpo().path("tratamientos").get(0).asText()).isEqualTo("Tratamiento ficticio");

        var historial = consultar("GET", "/api/mascotas/3/historial", null);
        assertThat(historial.estado()).isEqualTo(200);
        assertThat(historial.cuerpo().size()).isEqualTo(1);
        assertThat(historial.cuerpo().get(0).path("citaId").asLong()).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, id))
                .isEqualTo("COMPLETADA");
        assertThat(contarEventos("cita.completada:" + id)).isEqualTo(1);
        assertError(consultar("POST", "/api/citas/" + id + "/consulta", datosConsulta()), 409);
        assertError(consultar("PATCH", "/api/citas/" + id + "/cancelar", null), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial", Long.class)).isEqualTo(1);
        assertThat(contarEventos("cita.completada:" + id)).isEqualTo(1);

        var vacunas = consultar("GET", "/api/mascotas/3/proximas-vacunas", null);
        assertThat(vacunas.estado()).isEqualTo(200);
        assertThat(vacunas.cuerpo().isArray()).isTrue();
    }

    @Test
    void unaCitaCanceladaNoAdmiteConsulta() throws Exception {
        long id = agendar(3, 1, fecha.atTime(11, 30)).cuerpo().path("id").asLong();
        assertThat(consultar("PATCH", "/api/citas/" + id + "/cancelar", null).estado()).isEqualTo(200);
        assertError(consultar("POST", "/api/citas/" + id + "/consulta", datosConsulta()), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial", Long.class)).isZero();
    }

    @Test
    void falloAlPersistirElEventoRevierteLaConsultaYElCambioDeEstado() throws Exception {
        long id = agendar(3, 1, fecha.atTime(12, 0)).cuerpo().path("id").asLong();
        jdbc.execute("ALTER TABLE outbox_eventos ADD CONSTRAINT prueba_rechazar_completada "
                + "CHECK (routing_key <> 'cita.completada')");
        try {
            var respuesta = consultar("POST", "/api/citas/" + id + "/consulta", datosConsulta());
            assertThat(respuesta.estado()).isGreaterThanOrEqualTo(400);
            assertThat(jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, id))
                    .isEqualTo("AGENDADA");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial", Long.class)).isZero();
            assertThat(contarEventos("cita.completada:" + id)).isZero();
            assertThat(contarEventos("cita.agendada:" + id)).isEqualTo(1);
        } finally {
            jdbc.execute("ALTER TABLE outbox_eventos DROP CONSTRAINT prueba_rechazar_completada");
        }
    }

    @Test
    void dosSolicitudesConcurrentesSoloReservanUnaVez() throws Exception {
        var iniciar = new CountDownLatch(1);
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primera = ejecutor.submit(() -> {
                iniciar.await();
                return agendar(3, 1, fecha.atTime(13, 0));
            });
            var segunda = ejecutor.submit(() -> {
                iniciar.await();
                return agendar(4, 1, fecha.atTime(13, 0));
            });
            iniciar.countDown();
            var resultados = List.of(primera.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
            assertThat(resultados).extracting(Respuesta::estado).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos", Long.class)).isEqualTo(1);
    }

    @Test
    void mascotasCaidoDevuelve503YNoDejaUnaCitaParcial() throws Exception {
        when(mascotas.buscar(anyLong())).thenThrow(new DependenciaNoDisponibleException("Mascotas no disponible"));
        assertError(agendar(3, 1, fecha.atTime(13, 30)), 503);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos", Long.class)).isZero();
    }

    @Test
    void rechazaDatosFueraDelContratoConUnErrorEstructurado() throws Exception {
        assertError(consultar("GET", "/api/citas", null), 400);
        assertError(consultar("GET", "/api/citas?mascotaId=3&veterinarioId=1&fecha=" + fecha, null), 400);
        assertError(consultar("GET", "/api/citas?fecha=" + fecha, null), 400);
        assertError(consultar("GET", "/api/citas?mascotaId=3&veterinarioId=1", null), 400);
        assertError(consultar("GET", "/api/citas/0", null), 400);
        assertError(consultar("GET", "/api/citas?veterinarioId=0", null), 400);
        assertError(consultar("GET", "/api/veterinarios/1/disponibilidad?fecha=invalida", null), 400);
        assertError(agendar(-1, 1, fecha.atTime(9, 0)), 400);
        assertError(consultar("GET", "/api/citas?mascotaId=-5", null), 400);
        assertError(consultar("PATCH", "/api/citas/0/cancelar", null), 400);
        assertError(consultar("GET", "/api/mascotas/-1/historial", null), 400);
        assertError(agendar(3, 1, fecha.atTime(9, 15)), 400);
        assertError(agendar(3, 1, fecha.atTime(18, 0)), 400);
        assertError(agendar(3, 1, LocalDate.now(BOGOTA).minusDays(1).atTime(9, 0)), 400);
        assertError(consultar("POST", "/api/citas", Map.of(
                "mascotaId", 3, "veterinarioId", 1, "fechaHora", fecha.atTime(9, 0),
                "tipoServicio", "INEXISTENTE")), 400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas", Long.class)).isZero();
    }

    @Test
    void metodosYTiposDeContenidoNoAdmitidosConservanSuEstadoHttp() throws Exception {
        assertError(consultar("POST", "/api/veterinarios", Map.of()), 405);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/citas"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("contenido de texto"))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertError(new Respuesta(response.statusCode(), objectMapper.readTree(response.body())), 415);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas", Long.class)).isZero();
    }

    @Test
    void fechaFueraDelRangoDeNegocioDevuelve400() throws Exception {
        assertError(consultar("GET", "/api/veterinarios/1/disponibilidad?fecha=%2B999999999-12-31", null), 400);
    }

    @Test
    void recursosInexistentesDevuelven404() throws Exception {
        assertError(agendar(3, 999, fecha.atTime(9, 0)), 404);
        assertError(consultar("GET", "/api/veterinarios/999/disponibilidad?fecha=" + fecha, null), 404);
        assertError(consultar("PATCH", "/api/citas/999/cancelar", null), 404);
        assertError(consultar("GET", "/api/citas/999", null), 404);
        assertError(consultar("GET", "/api/citas?veterinarioId=999", null), 404);
        assertError(consultar("POST", "/api/citas/999/consulta", datosConsulta()), 404);
    }

    @Test
    void diagnosticoVacioNoCompletaLaCita() throws Exception {
        long id = agendar(3, 1, fecha.atTime(14, 0)).cuerpo().path("id").asLong();
        assertError(consultar("POST", "/api/citas/" + id + "/consulta",
                Map.of("tipoConsulta", "GENERAL", "diagnostico", " ")), 400);
        assertThat(jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, id))
                .isEqualTo("AGENDADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial", Long.class)).isZero();
    }

    private Respuesta agendar(long mascota, long veterinario, LocalDateTime fechaHora) throws Exception {
        return consultar("POST", "/api/citas", Map.of(
                "mascotaId", mascota, "veterinarioId", veterinario,
                "fechaHora", fechaHora, "tipoServicio", "CONSULTA_GENERAL"));
    }

    private Map<String, Object> datosConsulta() {
        return Map.of(
                "tipoConsulta", "GENERAL",
                "diagnostico", "Consulta de demostración",
                "vacunasAplicadas", List.of(),
                "tratamientos", List.of("Tratamiento ficticio"),
                "examenes", List.of(),
                "observaciones", "Datos exclusivos de la prueba",
                "proximoControl", fecha.plusWeeks(2));
    }

    private long contarEventos(String clave) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE clave_deduplicacion = ?",
                Long.class, clave);
    }

    private Respuesta consultar(String metodo, String ruta, Object cuerpo) throws Exception {
        var contenido = cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(metodo, contenido)
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new Respuesta(response.statusCode(), objectMapper.readTree(response.body()));
    }

    private void assertError(Respuesta respuesta, int estado) {
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(respuesta.cuerpo().path("error").asText()).isNotBlank();
        assertThat(respuesta.cuerpo().path("detalle").asText()).isNotBlank();
    }

    private record Respuesta(int estado, JsonNode cuerpo) {
    }
}
