package com.petvet.citas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Carga el historial de ejemplo como lo hace el Compose de demo y revisa lo que verá el front. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Timeout(30)
class DatosDemoIT {

    private static final String SEMILLA = "db/demo/R__historial_demo.sql";

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
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/demo");
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
    DataSource dataSource;

    @Autowired
    Environment entorno;

    @MockitoBean
    MascotaClient mascotas;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private LocalDate hoy;

    @BeforeEach
    void partirDeLaSemillaRecienCargada() {
        jdbc.update("DELETE FROM citas WHERE id < 900000");
        new ResourceDatabasePopulator(new ClassPathResource(SEMILLA)).execute(dataSource);
        hoy = jdbc.queryForObject("SELECT CURRENT_DATE", LocalDate.class);
        when(mascotas.buscar(anyLong())).thenAnswer(invocacion -> switch (invocacion.getArgument(0, Long.class).intValue()) {
            case 1 -> new Mascota(1L, "Tobby", Especie.PERRO, LocalDate.of(2021, 4, 18),
                    "Laura Méndez", "laura.mendez@correo.com");
            case 2 -> new Mascota(2L, "Misha", Especie.GATO, LocalDate.of(2023, 9, 2),
                    "Laura Méndez", "laura.mendez@correo.com");
            case 3 -> new Mascota(3L, "Kiwi", Especie.AVE, LocalDate.of(2024, 2, 11),
                    "Santiago Rojas", "santiago.rojas@correo.com");
            default -> throw new RecursoNoEncontradoException("No existe la mascota");
        });
    }

    @Test
    void lasVacunasDeLasMascotasDeEjemploQuedanAlDia() throws Exception {
        assertThat(proximasVacunas(1)).containsExactly(
                "Polivalente " + hoy.minusMonths(11).plusMonths(12),
                "Rabia " + hoy.minusMonths(10).plusMonths(12));
        assertThat(proximasVacunas(2)).containsExactly(
                "Rabia " + hoy.minusMonths(9).plusMonths(12),
                "Triple felina " + hoy.minusMonths(9).plusMonths(12));
        assertThat(proximasVacunas(3)).isEmpty();
    }

    @Test
    void elHistorialDeEjemploTraeConsultasVacunasTratamientosYExamenes() throws Exception {
        JsonNode historial = consultar("/api/mascotas/1/historial");

        assertThat(historial.size()).isEqualTo(3);
        assertThat(historial.findValuesAsText("diagnostico")).contains("Otitis leve en oído derecho.");
        assertThat(historial.toString()).contains("Polivalente", "Rabia", "Otoscopia",
                "Gotas óticas cada 12 horas por 7 días");
        assertThat(consultar("/api/citas?mascotaId=1").findValuesAsText("estado")).containsOnly("COMPLETADA");
    }

    @Test
    void elHistorialDeEjemploIncluyeUnaCirugiaConLaCirujana() throws Exception {
        JsonNode historial = consultar("/api/mascotas/2/historial");

        assertThat(historial.size()).isEqualTo(2);
        List<JsonNode> conCirugias = new ArrayList<>();
        historial.forEach(registro -> {
            if (!registro.path("cirugias").isEmpty()) {
                conCirugias.add(registro);
            }
        });
        assertThat(conCirugias).singleElement().satisfies(registro -> {
            assertThat(registro.path("cirugias")).extracting(JsonNode::asText).containsExactly("Esterilización");
            assertThat(registro.path("veterinarioId").asLong()).isEqualTo(3);
            assertThat(registro.path("tipoConsulta").asText()).isEqualTo("ESPECIALISTA");
        });
        assertThat(consultar("/api/mascotas/1/historial").findValues("cirugias"))
                .as("las consultas sin cirugías traen una lista vacía")
                .hasSize(3)
                .allMatch(cirugias -> cirugias.isArray() && cirugias.isEmpty());
    }

    @Test
    void repetirLaSemillaNoDuplicaNiFallaSiUnaCitaRealOcupaElMismoTurno() {
        new ResourceDatabasePopulator(new ClassPathResource(SEMILLA)).execute(dataSource);
        assertThat(contar("citas")).isEqualTo(6);
        assertThat(contar("registros_historial")).isEqualTo(6);
        assertThat(contar("historial_vacunas")).isEqualTo(4);

        jdbc.update("DELETE FROM registros_historial WHERE id = 900003");
        jdbc.update("UPDATE citas SET id = 1 WHERE id = 900003");
        new ResourceDatabasePopulator(new ClassPathResource(SEMILLA)).execute(dataSource);

        assertThat(contar("citas")).as("la cita real conserva el turno").isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM citas WHERE id = 900003", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registros_historial WHERE id = 900003", Long.class))
                .isZero();
        assertThat(contar("historial_examenes")).as("sin el examen de la consulta omitida").isEqualTo(2);
        assertThat(contar("historial_cirugias")).isEqualTo(1);
    }

    @Test
    void elServicioArrancaSobreElMismoVolumenAunqueLuegoNoSeCargueLaDemo() {
        String[] patrones = entorno.getRequiredProperty("spring.flyway.ignore-migration-patterns", String[].class);

        Flyway sinDemo = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .ignoreMigrationPatterns(patrones).load();
        assertThatCode(sinDemo::validate).doesNotThrowAnyException();
        assertThat(sinDemo.migrate().migrationsExecuted).isZero();

        assertThatThrownBy(() -> Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .load().validate())
                .as("sin el patrón, Flyway rechazaría la migración de demo ya aplicada")
                .isInstanceOf(FlywayValidateException.class);
    }

    private List<String> proximasVacunas(long mascotaId) throws Exception {
        JsonNode vacunas = consultar("/api/mascotas/" + mascotaId + "/proximas-vacunas");
        List<String> resultado = new ArrayList<>();
        vacunas.forEach(vacuna -> resultado.add(vacuna.path("vacuna").asText() + " " + vacuna.path("fechaLimite").asText()));
        return resultado;
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private JsonNode consultar(String ruta) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(ruta).isEqualTo(200);
        return objectMapper.readTree(response.body());
    }
}
