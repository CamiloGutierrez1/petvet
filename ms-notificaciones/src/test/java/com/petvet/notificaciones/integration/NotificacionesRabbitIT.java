package com.petvet.notificaciones.integration;

import static com.petvet.notificaciones.infrastructure.messaging.RabbitTopologia.COLA_CITAS;
import static com.petvet.notificaciones.infrastructure.messaging.RabbitTopologia.COLA_VACUNAS;
import static com.petvet.notificaciones.infrastructure.messaging.RabbitTopologia.DLQ_CITAS;
import static com.petvet.notificaciones.infrastructure.messaging.RabbitTopologia.DLQ_VACUNAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.petvet.notificaciones.application.NotificarCitaAgendadaUseCase;
import com.petvet.notificaciones.domain.EnviadorNotificaciones;
import com.petvet.notificaciones.domain.EnvioNoDisponibleException;
import com.petvet.notificaciones.infrastructure.messaging.RabbitTopologia;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Flujo completo con RabbitMQ y PostgreSQL reales y un SMTP en memoria (GreenMail) en un puerto
 * libre. Los mensajes se publican como lo hace el OutboxDispatcher de Citas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Timeout(60)
class NotificacionesRabbitIT {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
    private static final Duration ESPERA = Duration.ofSeconds(20);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @RegisterExtension
    static final GreenMailExtension SMTP = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void configurarDependencias(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.mail.host", () -> SMTP.getSmtp().getBindTo());
        registry.add("spring.mail.port", () -> SMTP.getSmtp().getPort());
        registry.add("spring.rabbitmq.listener.simple.retry.initial-interval", () -> "100ms");
        registry.add("spring.rabbitmq.listener.simple.retry.max-interval", () -> "200ms");
    }

    @LocalServerPort
    int puerto;

    @Autowired
    RabbitTemplate rabbit;

    @Autowired
    AmqpAdmin admin;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @MockitoSpyBean
    EnviadorNotificaciones enviador;

    @MockitoSpyBean
    NotificarCitaAgendadaUseCase notificarCitaAgendada;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private LocalDate hoy;

    @BeforeEach
    void limpiar() throws Exception {
        for (String cola : List.of(COLA_CITAS, COLA_VACUNAS, DLQ_CITAS, DLQ_VACUNAS)) {
            admin.purgeQueue(cola, false);
        }
        jdbc.execute("TRUNCATE TABLE notificaciones RESTART IDENTITY");
        SMTP.purgeEmailFromAllMailboxes();
        hoy = LocalDate.now(BOGOTA);
    }

    @Test
    void declaraAlArrancarLasColasDurablesConSuDlqYLosEnlacesDelContrato() throws Exception {
        assertThat(administracion("/api/exchanges/%2F/petvet.events").get("type").asText()).isEqualTo("topic");
        assertThat(administracion("/api/exchanges/%2F/petvet.notificaciones.dlx").get("type").asText())
                .isEqualTo("direct");
        for (String cola : List.of(COLA_CITAS, COLA_VACUNAS)) {
            JsonNode informacion = administracion("/api/queues/%2F/" + cola);
            assertThat(informacion.get("durable").asBoolean()).isTrue();
            assertThat(informacion.at("/arguments/x-dead-letter-exchange").asText())
                    .isEqualTo("petvet.notificaciones.dlx");
            assertThat(informacion.at("/arguments/x-dead-letter-routing-key").asText()).isEqualTo(cola + ".dlq");
            assertThat(administracion("/api/queues/%2F/" + cola + ".dlq").get("durable").asBoolean()).isTrue();
            assertThat(clavesDeEnlace("petvet.notificaciones.dlx", cola + ".dlq")).containsExactly(cola + ".dlq");
        }
        assertThat(clavesDeEnlace("petvet.events", COLA_CITAS)).containsExactly("cita.*");
        assertThat(clavesDeEnlace("petvet.events", COLA_VACUNAS)).containsExactly("vacuna.*");
    }

    @Test
    void elMismoMessageIdPublicadoDosVecesEnviaUnSoloCorreo() throws Exception {
        String repetido = UUID.randomUUID().toString();
        String cuerpo = citaAgendada(7, "laura.mendez@correo.com", hoy.plusDays(7).atTime(9, 30));
        publicar("cita.agendada", cuerpo, repetido);
        publicar("cita.agendada", cuerpo, repetido);
        // Llega por la misma cola después de los repetidos: cuando se guarda, ya se procesaron los dos.
        publicar("cita.agendada", citaAgendada(8, "santiago.rojas@correo.com", hoy.plusDays(8).atTime(15, 0)));

        await().atMost(ESPERA).until(() -> contarFilas() == 2 && SMTP.getReceivedMessages().length == 2);

        assertThat(destinatarios()).containsExactlyInAnyOrder("laura.mendez@correo.com", "santiago.rojas@correo.com");
        assertThat(contarFilas(repetido)).isEqualTo(1);
        verify(enviador, times(1)).enviar(eq("laura.mendez@correo.com"), anyString(), anyString());
        assertThat(contarMensajes(DLQ_CITAS)).isZero();
    }

    @Test
    void enviaLosTresCorreosPorSmtpEnUtf8ConElRemitenteDePetVet() throws Exception {
        LocalDateTime fechaCita = hoy.plusDays(7).atTime(9, 30);
        publicar("cita.agendada", citaAgendada(7, "laura.mendez@correo.com", fechaCita));
        publicar("cita.completada", citaCompletada(7, "laura.mendez@correo.com"));
        publicar("vacuna.proxima", vacunaProxima(4, "santiago.rojas@correo.com"));

        await().atMost(ESPERA).until(() -> contarFilas() == 3 && SMTP.getReceivedMessages().length == 3);

        Map<String, MimeMessage> porAsunto = new HashMap<>();
        for (MimeMessage correo : SMTP.getReceivedMessages()) {
            porAsunto.put(correo.getSubject(), correo);
        }
        assertThat(porAsunto).containsOnlyKeys("Confirmación de la cita de Tobby",
                "Resumen de la consulta de Tobby", "Recordatorio: vacuna Polivalente de Max");

        assertThat(texto(porAsunto.get("Confirmación de la cita de Tobby"))).isEqualTo("""
                Hola, Laura Méndez:

                La cita de vacunación de Tobby con la Dra. Paula Castaño quedó para el %s a las 09:30.

                Si no puede asistir, cancele la cita en PetVet para liberar el turno.

                Equipo PetVet""".formatted(FECHA.format(fechaCita)));
        assertThat(texto(porAsunto.get("Resumen de la consulta de Tobby"))).contains(
                "Diagnóstico: Otitis leve\n",
                "Vacunas aplicadas: Rabia\n",
                "Exámenes registrados en la consulta: Hemograma\n",
                "Próximo control: " + FECHA.format(hoy.plusDays(21)) + "\n",
                "- Polivalente: antes del " + FECHA.format(hoy.plusDays(30)) + "\n");
        MimeMessage recordatorio = porAsunto.get("Recordatorio: vacuna Polivalente de Max");
        assertThat(recordatorio.getRecipients(RecipientType.TO)[0].toString()).isEqualTo("santiago.rojas@correo.com");
        assertThat(texto(recordatorio)).contains("La vacuna Polivalente de su mascota Max va a vencer pronto. "
                + "Aplíquela antes del " + FECHA.format(hoy.plusDays(3)) + ".");

        for (MimeMessage correo : porAsunto.values()) {
            InternetAddress remitente = (InternetAddress) correo.getFrom()[0];
            assertThat(remitente.getPersonal()).isEqualTo("PetVet");
            assertThat(remitente.getAddress()).isEqualTo("notificaciones@petvet.local");
            assertThat(correo.getContentType()).containsIgnoringCase("charset=UTF-8");
        }
    }

    @Test
    void losMensajesVenenososVanASuDlqSinFrenarLosDemas() throws Exception {
        String truncado = UUID.randomUUID().toString();
        publicar("cita.agendada", "{\"citaId\": 7, \"fechaHora\": \"2026-", truncado);
        publicar("cita.completada", citaCompletada(7, "laura.mendez@correo.com"), null);
        publicar("cita.cancelada", "{\"citaId\": 7}");
        publicar("vacuna.proxima", "{\"vacuna\": \"Rabia\", \"mascotaId\": 4}");
        publicar("cita.agendada", citaAgendada(9, "santiago.rojas@correo.com", hoy.plusDays(2).atTime(10, 0)));

        await().atMost(ESPERA).until(() -> contarFilas() == 1 && contarMensajes(DLQ_CITAS) == 2
                && contarMensajes(DLQ_VACUNAS) == 1);

        // La clave desconocida se descarta sin ir a la DLQ y el mensaje válido sí se envía.
        assertThat(destinatarios()).containsExactly("santiago.rojas@correo.com");
        assertThat(contarMensajes(COLA_CITAS)).isZero();
        assertThat(contarMensajes(COLA_VACUNAS)).isZero();
        Message primero = rabbit.receive(DLQ_CITAS, 5_000);
        assertThat(primero).isNotNull();
        assertThat(primero.getMessageProperties().getMessageId()).isEqualTo(truncado);
        Map<String, ?> muerte = primero.getMessageProperties().getXDeathHeader().getFirst();
        assertThat(String.valueOf(muerte.get("reason"))).isEqualTo("rejected");
        assertThat(String.valueOf(muerte.get("queue"))).isEqualTo(COLA_CITAS);
        Message segundo = rabbit.receive(DLQ_CITAS, 5_000);
        assertThat(segundo.getMessageProperties().getMessageId()).isNull();
        assertThat(segundo.getMessageProperties().getType()).isEqualTo("cita.completada");
    }

    /** Un mensaje que no se puede procesar falla igual en cada intento: el contenedor no lo reintenta. */
    @Test
    void unRechazoDefinitivoPasaALaDlqSinReintentos() throws Exception {
        doThrow(new AmqpRejectAndDontRequeueException("El mensaje no se puede procesar"))
                .when(notificarCitaAgendada).ejecutar(eq("rechazo-definitivo"), any());
        publicar("cita.agendada", citaAgendada(12, "laura.mendez@correo.com", hoy.plusDays(3).atTime(8, 0)),
                "rechazo-definitivo");

        await().atMost(ESPERA).until(() -> contarMensajes(DLQ_CITAS) == 1);
        verify(notificarCitaAgendada, times(1)).ejecutar(eq("rechazo-definitivo"), any());
        assertThat(contarFilas()).isZero();
    }

    @Test
    void siElSmtpFallaReintentaCincoVecesYElMensajeQuedaEnLaDlqParaReprocesarlo() throws Exception {
        doThrow(new EnvioNoDisponibleException("SMTP caído", null))
                .when(enviador).enviar(eq("falla@example.test"), anyString(), anyString());
        String messageId = publicar("cita.completada", citaCompletada(11, "falla@example.test"));

        await().atMost(ESPERA).until(() -> contarMensajes(DLQ_CITAS) == 1);
        verify(enviador, times(5)).enviar(eq("falla@example.test"), anyString(), anyString());
        assertThat(contarFilas()).isZero();
        assertThat(SMTP.getReceivedMessages()).isEmpty();

        // Con el SMTP de vuelta, el mensaje se mueve de la DLQ a su cola por el exchange por defecto.
        doCallRealMethod().when(enviador).enviar(anyString(), anyString(), anyString());
        Message fallido = rabbit.receive(DLQ_CITAS, 5_000);
        assertThat(fallido.getMessageProperties().getMessageId()).isEqualTo(messageId);
        rabbit.send("", COLA_CITAS, fallido);

        await().atMost(ESPERA).until(() -> contarFilas(messageId) == 1 && SMTP.getReceivedMessages().length == 1);
        assertThat(destinatarios()).containsExactly("falla@example.test");
        assertThat(contarMensajes(DLQ_CITAS)).isZero();
    }

    @Test
    void laApiListaPrimeroLaUltimaNotificacionYValidaElLimite() throws Exception {
        publicar("cita.agendada", citaAgendada(7, "laura.mendez@correo.com", hoy.plusDays(7).atTime(9, 30)));
        await().atMost(ESPERA).until(() -> contarFilas() == 1);
        String segundo = publicar("vacuna.proxima", vacunaProxima(4, "santiago.rojas@correo.com"));
        await().atMost(ESPERA).until(() -> contarFilas() == 2);

        HttpResponse<String> respuesta = get("/api/notificaciones?limite=1");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        JsonNode lista = json.readTree(respuesta.body());
        assertThat(lista.size()).isEqualTo(1);
        JsonNode ultima = lista.get(0);
        assertThat(ultima.get("id").isIntegralNumber()).isTrue();
        assertThat(ultima.get("messageId").asText()).isEqualTo(segundo);
        assertThat(ultima.get("tipo").asText()).isEqualTo("VACUNA_PROXIMA");
        assertThat(ultima.get("destino").asText()).isEqualTo("santiago.rojas@correo.com");
        assertThat(ultima.get("asunto").asText()).isEqualTo("Recordatorio: vacuna Polivalente de Max");
        assertThat(ultima.get("cuerpo").asText()).contains("va a vencer pronto");
        assertThat(Instant.parse(ultima.get("creadaEn").asText()))
                .isBeforeOrEqualTo(Instant.parse(ultima.get("enviadaEn").asText()));
        assertThat(json.readTree(get("/api/notificaciones").body()).size()).isEqualTo(2);

        HttpResponse<String> invalido = get("/api/notificaciones?limite=0");
        assertThat(invalido.statusCode()).isEqualTo(400);
        assertThat(json.readTree(invalido.body())).isEqualTo(json.readTree("""
                {"error": "DATOS_INVALIDOS", "detalle": "El límite debe estar entre 1 y 100"}"""));
        assertThat(get("/api/notificaciones?limite=abc").statusCode()).isEqualTo(400);
    }

    /** Lo mismo que revisa scripts/verificar_local.py en cada servicio del perfil app. */
    @Test
    void exponeSaludOpenApiSwaggerYCorsComoLosDemasServicios() throws Exception {
        HttpResponse<String> salud = get("/actuator/health");
        assertThat(salud.statusCode()).isEqualTo(200);
        assertThat(json.readTree(salud.body()).get("status").asText()).isEqualTo("UP");

        JsonNode documento = json.readTree(get("/v3/api-docs").body());
        assertThat(documento.get("openapi").asText()).startsWith("3.");
        assertThat(documento.at("/info/title").asText()).isEqualTo("PetVet — Notificaciones");
        assertThat(documento.at("/paths/~1api~1notificaciones/get").isObject()).isTrue();
        assertThat(get("/swagger-ui/index.html").body()).contains("Swagger UI");

        HttpRequest preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/v3/api-docs"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Content-Type")
                .build();
        HttpResponse<String> cors = http.send(preflight, HttpResponse.BodyHandlers.ofString());
        assertThat(cors.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
    }

    private String publicar(String clave, String cuerpo) {
        String messageId = UUID.randomUUID().toString();
        publicar(clave, cuerpo, messageId);
        return messageId;
    }

    /** Mismas propiedades que el OutboxDispatcher de Citas. Un messageId nulo simula otro publicador. */
    private void publicar(String clave, String cuerpo, String messageId) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        propiedades.setMessageId(messageId);
        propiedades.setTimestamp(new Date());
        propiedades.setType(clave);
        rabbit.send(RabbitTopologia.EXCHANGE, clave,
                new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades));
    }

    private static String citaAgendada(long citaId, String correo, LocalDateTime fechaHora) {
        return """
                {"citaId": %d, "fechaHora": "%s", "duenoEmail": "%s", "duenoNombre": "Laura Méndez", \
                "tipoServicio": "VACUNACION", "mascotaNombre": "Tobby", "veterinarioNombre": "Dra. Paula Castaño"}"""
                .formatted(citaId, fechaHora.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), correo);
    }

    private String citaCompletada(long citaId, String correo) {
        return """
                {"citaId": %d, "examenes": ["Hemograma"], "duenoEmail": "%s", "diagnostico": "Otitis leve", \
                "mascotaNombre": "Tobby", "proximoControl": "%s", "proximasVacunas": [{"vacuna": "Polivalente", \
                "fechaLimite": "%s"}], "vacunasAplicadas": ["Rabia"]}"""
                .formatted(citaId, correo, hoy.plusDays(21), hoy.plusDays(30));
    }

    private String vacunaProxima(long mascotaId, String correo) {
        return """
                {"vacuna": "Polivalente", "mascotaId": %d, "duenoEmail": "%s", "fechaLimite": "%s", \
                "mascotaNombre": "Max"}""".formatted(mascotaId, correo, hoy.plusDays(3));
    }

    private static String texto(MimeMessage correo) throws Exception {
        return ((String) correo.getContent()).replace("\r\n", "\n").strip();
    }

    private static List<String> destinatarios() throws Exception {
        List<String> destinatarios = new ArrayList<>();
        for (MimeMessage correo : SMTP.getReceivedMessages()) {
            destinatarios.add(correo.getRecipients(RecipientType.TO)[0].toString());
        }
        return destinatarios;
    }

    private long contarFilas() {
        return jdbc.queryForObject("SELECT count(*) FROM notificaciones", Long.class);
    }

    private long contarFilas(String messageId) {
        return jdbc.queryForObject("SELECT count(*) FROM notificaciones WHERE message_id = ?", Long.class, messageId);
    }

    private int contarMensajes(String cola) {
        QueueInformation informacion = admin.getQueueInfo(cola);
        return informacion == null ? -1 : informacion.getMessageCount();
    }

    /** API HTTP de administración de RabbitMQ, la misma del panel 15672. */
    private JsonNode administracion(String ruta) throws Exception {
        String credenciales = Base64.getEncoder().encodeToString(
                (RABBIT.getAdminUsername() + ":" + RABBIT.getAdminPassword()).getBytes(StandardCharsets.UTF_8));
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(RABBIT.getHttpUrl() + ruta))
                .header("Authorization", "Basic " + credenciales)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).as("GET %s", ruta).isEqualTo(200);
        return json.readTree(respuesta.body());
    }

    private List<String> clavesDeEnlace(String exchange, String cola) throws Exception {
        List<String> claves = new ArrayList<>();
        for (JsonNode enlace : administracion("/api/bindings/%2F/e/" + exchange + "/q/" + cola)) {
            claves.add(enlace.get("routing_key").asText());
        }
        return claves;
    }

    private HttpResponse<String> get(String ruta) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return http.send(pedido, HttpResponse.BodyHandlers.ofString());
    }
}
