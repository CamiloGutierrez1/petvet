package com.petvet.mascotas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petvet.mascotas.domain.ConflictoException;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.RegistroDuenos;
import java.math.BigDecimal;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Timeout(30)
class MascotasApiIT {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String CORREO_REPETIDO = "Ya existe un dueño registrado con ese correo.";
    private static final String ID_INVALIDO = "El identificador de la ruta debe ser un número entero positivo.";
    private static final Map<Integer, String> CODIGOS = Map.of(
            400, "DATOS_INVALIDOS", 404, "RECURSO_NO_ENCONTRADO", 405, "METODO_NO_PERMITIDO",
            409, "CONFLICTO", 415, "CONTENIDO_NO_SOPORTADO");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configurarDependencias(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Cada prueba vuelve a aplicar las migraciones: datos de ejemplo intactos e identidades en 3 y 4.
        registry.add("spring.flyway.clean-disabled", () -> false);
    }

    @LocalServerPort
    int puerto;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    @Autowired
    RegistroDuenos registroDuenos;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void reiniciarBase() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void laFichaDeTobbyEsLaFichaPlanaQueConsumeCitas() throws Exception {
        var tobby = consultar("GET", "/api/mascotas/1", null);
        assertThat(tobby.estado()).isEqualTo(200);
        assertThat(tobby.texto()).isEqualTo("{\"id\":1,\"nombre\":\"Tobby\",\"especie\":\"PERRO\","
                + "\"raza\":\"Golden Retriever\",\"fechaNacimiento\":\"2021-04-18\",\"peso\":31.5,"
                + "\"fotoUrl\":\"/mascotas/tobby.jpg\",\"duenoId\":1,\"duenoNombre\":\"Laura Méndez\","
                + "\"duenoEmail\":\"laura.mendez@correo.com\"}");

        var kiwi = consultar("GET", "/api/mascotas/3", null);
        assertThat(kiwi.texto()).contains("\"peso\":0.04,", "\"duenoNombre\":\"Santiago Rojas\"");

        assertError(consultar("GET", "/api/mascotas/999", null), 404, "No existe la mascota 999");
        assertError(consultar("GET", "/api/mascotas/abc", null), 400, ID_INVALIDO);
        assertError(consultar("GET", "/api/mascotas/0", null), 400, ID_INVALIDO);
        assertError(consultar("GET", "/api/ruta-inexistente", null), 404, "La ruta no existe.");
    }

    @Test
    void losPrimerosRegistrosNuevosTomanLosIdsTresYCuatro() throws Exception {
        var dueno = consultar("POST", "/api/duenos", Map.of("nombre", "Ana Prueba", "email", "ana.prueba@correo.com"));
        assertThat(dueno.estado()).isEqualTo(201);
        assertThat(dueno.cuerpo().path("id").asLong()).isEqualTo(3);
        assertThat(dueno.ubicacion()).endsWith("/api/duenos/3");

        var mascota = consultar("POST", "/api/duenos/3/mascotas",
                mascota("Max", "PERRO", LocalDate.of(2025, 6, 1), "100"));
        assertThat(mascota.estado()).isEqualTo(201);
        assertThat(mascota.ubicacion()).endsWith("/api/mascotas/4");
        assertThat(mascota.texto()).isEqualTo("{\"id\":4,\"nombre\":\"Max\",\"especie\":\"PERRO\",\"raza\":\"Criollo\","
                + "\"fechaNacimiento\":\"2025-06-01\",\"peso\":100,\"fotoUrl\":null,\"duenoId\":3}");
    }

    @Test
    void registraUnDuenoYRechazaElCorreoRepetidoAunqueCambienMayusculasYEspacios() throws Exception {
        var creado = consultar("POST", "/api/duenos", Map.of(
                "nombre", "  Carolina Díaz ", "documento", "1000000003", "email", "  Carolina.Diaz@Correo.com ",
                "telefono", "3001234567", "direccion", "Calle 1 # 2-3, Bogotá"));
        assertThat(creado.estado()).isEqualTo(201);
        assertThat(creado.texto()).isEqualTo("{\"id\":3,\"nombre\":\"Carolina Díaz\",\"documento\":\"1000000003\","
                + "\"email\":\"carolina.diaz@correo.com\",\"telefono\":\"3001234567\","
                + "\"direccion\":\"Calle 1 # 2-3, Bogotá\"}");
        assertThat(consultar("GET", "/api/duenos/3", null).texto()).isEqualTo(creado.texto());

        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "Otra persona", "email", "CAROLINA.DIAZ@correo.com")),
                409, CORREO_REPETIDO);
        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "Laura", "email", " Laura.Mendez@Correo.COM ")),
                409, CORREO_REPETIDO);

        var sinOpcionales = consultar("POST", "/api/duenos", Map.of(
                "nombre", "Pedro Gómez", "email", "pedro.gomez@correo.com", "telefono", "", "documento", " "));
        assertThat(sinOpcionales.estado()).isEqualTo(201);
        assertThat(sinOpcionales.cuerpo().path("telefono").isNull()).isTrue();
        assertThat(sinOpcionales.cuerpo().path("documento").isNull()).isTrue();
        assertThat(sinOpcionales.cuerpo().path("direccion").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM duenos", Long.class)).isEqualTo(4);
    }

    @Test
    void correoOTelefonoInvalidosResponden400EnEspanol() throws Exception {
        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "Ana", "email", "ana@clinica")), 400,
                "El correo del dueño no es válido. Ejemplo: ana@correo.com.");
        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "Ana", "email", "ana@correo.com",
                "telefono", "310-456-78")), 400, "El teléfono debe tener solo números, entre 7 y 10 dígitos.");
        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "Ana", "email", "ana@correo.com",
                "telefono", "12345")), 400, "El teléfono debe tener solo números, entre 7 y 10 dígitos.");
        assertError(consultar("POST", "/api/duenos", Map.of("email", "ana@correo.com")), 400,
                "El nombre del dueño es obligatorio.");
        // Un texto más largo que la columna se rechaza antes de llegar a la base: 400 y no 409.
        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "A".repeat(101), "email", "ana@correo.com")),
                400, "El nombre del dueño admite máximo 100 caracteres.");
        assertError(consultar("POST", "/api/duenos", Map.of("nombre", "Ana")), 400, "El correo del dueño es obligatorio.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM duenos", Long.class)).isEqualTo(2);
    }

    @Test
    void registraMascotasYValidaFechaPesoEspecieYDueno() throws Exception {
        LocalDate hoy = LocalDate.now(BOGOTA);
        var luna = consultar("POST", "/api/duenos/2/mascotas", mascota(" Luna ", "GATO", hoy, "3.25"));
        assertThat(luna.estado()).isEqualTo(201);
        assertThat(luna.cuerpo().path("id").asLong()).isEqualTo(4);
        assertThat(luna.cuerpo().path("nombre").asText()).isEqualTo("Luna");
        assertThat(luna.cuerpo().path("duenoId").asLong()).isEqualTo(2);
        assertThat(luna.cuerpo().path("fotoUrl").isNull()).isTrue();
        assertThat(luna.texto()).contains("\"peso\":3.25,");

        assertError(consultar("POST", "/api/duenos/2/mascotas", mascota("Sol", "GATO", hoy.plusDays(1), "3")), 400,
                "La fecha de nacimiento no puede ser futura.");
        assertError(consultar("POST", "/api/duenos/99/mascotas", mascota("Sol", "GATO", hoy, "3")), 404,
                "No existe el dueño 99");
        assertError(consultar("POST", "/api/duenos/2/mascotas", mascota("Sol", "AVE", hoy, "0.035")), 400,
                "El peso admite hasta 4 enteros y 2 decimales, por ejemplo 31.5 o 0.04.");
        assertError(consultar("POST", "/api/duenos/2/mascotas", mascota("Sol", "AVE", hoy, "0")), 400,
                "El peso debe ser mayor que 0.");
        assertError(consultar("POST", "/api/duenos/2/mascotas", mascota("Sol", "DINOSAURIO", hoy, "3")), 400,
                "La especie debe ser PERRO, GATO, AVE u OTRO.");
        assertError(enviarJson("POST", "/api/duenos/2/mascotas", "{\"nombre\": \"Sol\", "), 400,
                "Revise el JSON enviado: la fecha va en formato ISO, por ejemplo 2021-04-18, y el peso es un número.");
        assertError(consultar("POST", "/api/duenos/2/mascotas", Map.of("nombre", "Sol", "especie", "AVE",
                "raza", "Canario", "fechaNacimiento", "11/02/2024", "peso", 1)), 400,
                "Revise el JSON enviado: la fecha va en formato ISO, por ejemplo 2021-04-18, y el peso es un número.");

        var deSantiago = consultar("GET", "/api/duenos/2/mascotas", null);
        assertThat(deSantiago.cuerpo().findValuesAsText("nombre")).containsExactly("Kiwi", "Luna");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascotas", Long.class)).isEqualTo(4);
    }

    @Test
    void elPutCambiaLosCincoCamposYConservaLaFotoYElDueno() throws Exception {
        var cambios = Map.<String, Object>of("nombre", "Tobby", "especie", "PERRO", "raza", "Golden Retriever",
                "fechaNacimiento", "2021-04-18", "peso", new BigDecimal("32.40"),
                "duenoId", 2, "fotoUrl", "http://otro.sitio/foto.jpg");
        var editada = consultar("PUT", "/api/mascotas/1", cambios);
        assertThat(editada.estado()).isEqualTo(200);
        assertThat(editada.texto()).isEqualTo("{\"id\":1,\"nombre\":\"Tobby\",\"especie\":\"PERRO\","
                + "\"raza\":\"Golden Retriever\",\"fechaNacimiento\":\"2021-04-18\",\"peso\":32.4,"
                + "\"fotoUrl\":\"/mascotas/tobby.jpg\",\"duenoId\":1}");
        assertThat(consultar("GET", "/api/mascotas/1", null).cuerpo().path("duenoEmail").asText())
                .isEqualTo("laura.mendez@correo.com");

        assertError(consultar("PUT", "/api/mascotas/999", mascota("Max", "PERRO", LocalDate.of(2025, 6, 1), "12")),
                404, "No existe la mascota 999");
        assertError(consultar("PUT", "/api/mascotas/1", mascota("Tobby", "PERRO", LocalDate.now(BOGOTA).plusDays(1),
                "31.5")), 400, "La fecha de nacimiento no puede ser futura.");
    }

    @Test
    void listaLosDuenosYLasMascotasDeCadaUno() throws Exception {
        var duenos = consultar("GET", "/api/duenos", null);
        assertThat(duenos.estado()).isEqualTo(200);
        assertThat(duenos.cuerpo().isArray()).isTrue();
        assertThat(duenos.cuerpo().findValuesAsText("nombre")).containsExactly("Laura Méndez", "Santiago Rojas");

        var laura = consultar("GET", "/api/duenos/1", null);
        assertThat(laura.texto()).isEqualTo("{\"id\":1,\"nombre\":\"Laura Méndez\",\"documento\":\"1000000001\","
                + "\"email\":\"laura.mendez@correo.com\",\"telefono\":\"3104567812\","
                + "\"direccion\":\"Calle 10 # 20-30, Bogotá\"}");
        assertError(consultar("GET", "/api/duenos/999", null), 404, "No existe el dueño 999");

        var deLaura = consultar("GET", "/api/duenos/1/mascotas", null);
        assertThat(deLaura.estado()).isEqualTo(200);
        assertThat(deLaura.cuerpo().findValuesAsText("nombre")).containsExactly("Tobby", "Misha");
        assertThat(deLaura.cuerpo().get(1).path("peso").asText()).isEqualTo("4.2");
        assertError(consultar("GET", "/api/duenos/999/mascotas", null), 404, "No existe el dueño 999");
    }

    @Test
    void elCorreoRepetidoQueSoloDetectaLaRestriccionUniqueTambienEsConflicto() throws Exception {
        assertThatThrownBy(() -> registroDuenos.guardar(
                new Dueno(null, "Laura Copia", null, "laura.mendez@correo.com", null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(CORREO_REPETIDO);

        var iniciar = new CountDownLatch(1);
        List<Respuesta> respuestas = new ArrayList<>();
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Respuesta>> envios = new ArrayList<>();
            for (int intento = 0; intento < 6; intento++) {
                String correo = intento % 2 == 0 ? "Paula.Rios@correo.com" : " paula.rios@correo.com ";
                Callable<Respuesta> envio = () -> {
                    iniciar.await();
                    return consultar("POST", "/api/duenos", Map.of("nombre", "Paula Ríos", "email", correo));
                };
                envios.add(ejecutor.submit(envio));
            }
            iniciar.countDown();
            for (var envio : envios) {
                respuestas.add(envio.get());
            }
        }

        assertThat(respuestas).filteredOn(respuesta -> respuesta.estado() == 201).hasSize(1);
        assertThat(respuestas).filteredOn(respuesta -> respuesta.estado() != 201)
                .hasSize(5)
                .allSatisfy(respuesta -> assertError(respuesta, 409, CORREO_REPETIDO));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM duenos WHERE email = 'paula.rios@correo.com'", Long.class))
                .isEqualTo(1);
    }

    @Test
    void permiteElPreflightDelFrontYPublicaOpenApi() throws Exception {
        var preflight = http.send(HttpRequest.newBuilder(uri("/v3/api-docs"))
                .timeout(Duration.ofSeconds(10))
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.discarding());
        assertThat(preflight.statusCode()).isEqualTo(200);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");

        var documentacion = consultar("GET", "/v3/api-docs", null);
        assertThat(documentacion.estado()).isEqualTo(200);
        assertThat(documentacion.cuerpo().path("info").path("title").asText()).contains("PetVet");
        assertThat(documentacion.cuerpo().path("paths").has("/api/mascotas/{id}")).isTrue();
    }

    @Test
    void metodosYTiposDeContenidoNoAdmitidosConservanSuEstadoHttp() throws Exception {
        assertError(consultar("DELETE", "/api/mascotas/1", null), 405, "El método HTTP no está disponible en esta ruta.");
        String noSoportado = "Envíe los datos como application/json.";
        assertError(enviar(HttpRequest.newBuilder(uri("/api/duenos"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("contenido de texto"))), 415, noSoportado);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM duenos", Long.class)).isEqualTo(2);
    }

    private static Map<String, Object> mascota(String nombre, String especie, LocalDate fechaNacimiento, String peso) {
        return Map.of("nombre", nombre, "especie", especie, "raza", "Criollo",
                "fechaNacimiento", fechaNacimiento.toString(), "peso", new BigDecimal(peso));
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private Respuesta consultar(String metodo, String ruta, Object cuerpo) throws Exception {
        return enviarJson(metodo, ruta, cuerpo == null ? null : objectMapper.writeValueAsString(cuerpo));
    }

    private Respuesta enviarJson(String metodo, String ruta, String json) throws Exception {
        var contenido = json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json);
        return enviar(HttpRequest.newBuilder(uri(ruta))
                .header("Content-Type", "application/json")
                .method(metodo, contenido));
    }

    private Respuesta enviar(HttpRequest.Builder request) throws Exception {
        var response = http.send(request.timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
        return new Respuesta(response.statusCode(), response.body(), objectMapper.readTree(response.body()),
                response.headers().firstValue("Location").orElse(""));
    }

    private void assertError(Respuesta respuesta, int estado, String detalle) {
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(respuesta.cuerpo().size()).isEqualTo(2);
        assertThat(respuesta.cuerpo().path("error").asText()).isEqualTo(CODIGOS.get(estado));
        assertThat(respuesta.cuerpo().path("detalle").asText()).isEqualTo(detalle);
    }

    private record Respuesta(int estado, String texto, JsonNode cuerpo, String ubicacion) {
    }
}
