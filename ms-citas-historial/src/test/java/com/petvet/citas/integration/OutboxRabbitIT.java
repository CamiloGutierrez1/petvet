package com.petvet.citas.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petvet.citas.application.VacunasService;
import com.petvet.citas.domain.CitaRepository;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import com.petvet.citas.infrastructure.RecordatoriosVacunasJob;
import com.petvet.citas.infrastructure.messaging.OutboxDispatcher;
import com.petvet.citas.infrastructure.messaging.RabbitConfiguration;
import com.rabbitmq.client.GetResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
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
class OutboxRabbitIT {

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
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> true);
        registry.add("spring.rabbitmq.template.mandatory", () -> true);
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
    RabbitTemplate rabbit;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    VacunasService vacunas;

    @Autowired
    CitaRepository citas;

    @MockitoBean
    MascotaClient mascotas;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final List<String> colas = new ArrayList<>();
    private RabbitAdmin admin;
    private OutboxDispatcher dispatcher;
    private LocalDate hoy;

    @BeforeEach
    void prepararEscenario() {
        jdbc.execute("TRUNCATE TABLE outbox_eventos, registros_historial, citas RESTART IDENTITY CASCADE");
        hoy = LocalDate.now(BOGOTA);
        when(mascotas.buscar(anyLong())).thenAnswer(invocation -> new Mascota(
                invocation.getArgument(0, Long.class), "Mascota Demo", Especie.PERRO,
                hoy.minusMonths(6), "Dueño Demo", "demo@example.test"));
        admin = new RabbitAdmin(rabbit.getConnectionFactory());
        admin.declareExchange(new TopicExchange(RabbitConfiguration.EXCHANGE, true, false));
        dispatcher = new OutboxDispatcher(jdbc, rabbit, transactionManager);
    }

    @AfterEach
    void eliminarColasDePrueba() {
        for (String nombre : colas) {
            admin.deleteQueue(nombre);
        }
        colas.clear();
    }

    @Test
    void eventoPersistenteEsperaEnLaColaHastaQueElConsumidorLoLee() throws Exception {
        String cola = declararCola("cita.*");
        long citaId = agendar();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE enviado_en IS NULL",
                Long.class)).isEqualTo(1);
        assertThat(leerMensaje(cola)).isNull();

        dispatcher.despachar();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE enviado_en IS NOT NULL",
                Long.class)).isEqualTo(1);
        GetResponse mensaje = leerMensaje(cola);
        assertThat(mensaje).isNotNull();
        assertThat(mensaje.getProps().getDeliveryMode()).isEqualTo(2);
        assertThat(mensaje.getProps().getContentType()).isEqualTo("application/json");
        UUID eventoId = UUID.fromString(mensaje.getProps().getMessageId());
        assertThat(jdbc.queryForObject("SELECT id FROM outbox_eventos WHERE clave_deduplicacion = ?",
                UUID.class, "cita.agendada:" + citaId)).isEqualTo(eventoId);
        JsonNode contenido = objectMapper.readTree(mensaje.getBody());
        assertThat(contenido.path("citaId").asLong()).isEqualTo(citaId);
        assertThat(contenido.path("mascotaNombre").asText()).isEqualTo("Mascota Demo");
        assertThat(contenido.path("duenoEmail").asText()).isEqualTo("demo@example.test");
        assertThat(contenido.path("tipoServicio").asText()).isEqualTo("CONSULTA_GENERAL");
        dispatcher.despachar();
        assertThat(leerMensaje(cola)).isNull();
    }

    @Test
    void ausenciaDeColaConservaElEventoYLoRecuperaConElMismoIdentificador() throws Exception {
        long citaId = agendar();
        UUID eventoId = jdbc.queryForObject("SELECT id FROM outbox_eventos WHERE clave_deduplicacion = ?",
                UUID.class, "cita.agendada:" + citaId);

        dispatcher.despachar();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE enviado_en IS NULL",
                Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT intentos FROM outbox_eventos WHERE id = ?",
                Integer.class, eventoId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM citas WHERE id = ?", String.class, citaId))
                .isEqualTo("AGENDADA");

        String cola = declararCola("cita.*");
        jdbc.update("UPDATE outbox_eventos SET proximo_intento = CURRENT_TIMESTAMP WHERE id = ?", eventoId);
        dispatcher.despachar();

        GetResponse recuperado = leerMensaje(cola);
        assertThat(recuperado).isNotNull();
        assertThat(recuperado.getProps().getMessageId()).isEqualTo(eventoId.toString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE enviado_en IS NULL",
                Long.class)).isZero();
        assertThat(leerMensaje(cola)).isNull();
    }

    @Test
    void recordatorioSeDeduplicaYPublicaLaFechaDeLaVacuna() throws Exception {
        LocalDate nacimiento = hoy.minusMonths(2).plusDays(3);
        LocalDate vencimiento = nacimiento.plusMonths(2);
        when(mascotas.buscar(3)).thenReturn(new Mascota(3L, "Cachorro Demo", Especie.PERRO,
                nacimiento, "Dueño Demo", "demo@example.test"));
        String colaCitas = declararCola("cita.*");
        String colaVacunas = declararCola("vacuna.*");
        long citaId = agendar();
        var consulta = enviar("POST", "/api/citas/" + citaId + "/consulta", Map.of(
                "tipoConsulta", "GENERAL", "diagnostico", "Control ficticio",
                "vacunasAplicadas", List.of(), "tratamientos", List.of(), "examenes", List.of()));
        assertThat(consulta.statusCode()).isEqualTo(201);

        vacunas.publicarRecordatorios(3);
        vacunas.publicarRecordatorios(3);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE routing_key = 'vacuna.proxima'",
                Long.class)).isEqualTo(1);
        dispatcher.despachar();

        GetResponse recordatorio = leerMensaje(colaVacunas);
        assertThat(recordatorio).isNotNull();
        assertThat(recordatorio.getProps().getDeliveryMode()).isEqualTo(2);
        var contenido = objectMapper.readTree(recordatorio.getBody());
        assertThat(contenido.path("mascotaId").asLong()).isEqualTo(3);
        assertThat(contenido.path("mascotaNombre").asText()).isEqualTo("Cachorro Demo");
        assertThat(contenido.path("duenoEmail").asText()).isEqualTo("demo@example.test");
        assertThat(contenido.path("vacuna").asText()).isEqualTo("Polivalente");
        assertThat(contenido.path("fechaLimite").asText()).isEqualTo(vencimiento.toString());
        assertThat(leerMensaje(colaVacunas)).isNull();
        assertThat(leerMensaje(colaCitas)).isNotNull();
        assertThat(leerMensaje(colaCitas)).isNotNull();

        vacunas.publicarRecordatorios(3);
        dispatcher.despachar();
        assertThat(leerMensaje(colaVacunas)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE routing_key = 'vacuna.proxima'",
                Long.class)).isEqualTo(1);
    }

    @Test
    void tareaProgramadaIncluyeMascotasAgendadasQueAunNoTienenHistorial() throws Exception {
        LocalDate nacimiento = hoy.minusMonths(2).plusDays(3);
        when(mascotas.buscar(3)).thenReturn(new Mascota(3L, "Cachorro sin historial", Especie.PERRO,
                nacimiento, "Dueño Demo", "demo@example.test"));
        agendar();
        var job = new RecordatoriosVacunasJob(citas, vacunas);

        job.ejecutar();
        job.ejecutar();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_eventos WHERE routing_key = 'vacuna.proxima'",
                Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT payload ->> 'fechaLimite' FROM outbox_eventos "
                + "WHERE routing_key = 'vacuna.proxima'", String.class)).isEqualTo(nacimiento.plusMonths(2).toString());
    }

    private String declararCola(String routingKey) {
        var cola = new Queue("petvet.prueba." + UUID.randomUUID(), true, false, false);
        admin.declareQueue(cola);
        admin.declareBinding(BindingBuilder.bind(cola)
                .to(new TopicExchange(RabbitConfiguration.EXCHANGE, true, false)).with(routingKey));
        colas.add(cola.getName());
        return cola.getName();
    }

    private GetResponse leerMensaje(String cola) {
        return rabbit.execute(channel -> channel.basicGet(cola, true));
    }

    private long agendar() throws Exception {
        var respuesta = enviar("POST", "/api/citas", Map.of(
                "mascotaId", 3, "veterinarioId", 1, "fechaHora", hoy.plusDays(4).atTime(9, 0),
                "tipoServicio", "CONSULTA_GENERAL"));
        assertThat(respuesta.statusCode()).isEqualTo(201);
        return objectMapper.readTree(respuesta.body()).path("id").asLong();
    }

    private HttpResponse<String> enviar(String metodo, String ruta, Object cuerpo) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo)))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
