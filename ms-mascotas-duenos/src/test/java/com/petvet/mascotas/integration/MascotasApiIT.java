package com.petvet.mascotas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.petvet.mascotas.domain.ConflictoException;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.RegistroDuenos;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
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
    private static final String URL_BASE_FOTOS = "http://mascotas.prueba";
    private static final String ORIGEN_FRONT = "http://localhost:5173";
    private static final String CORREO_REPETIDO = "Ya existe un dueño registrado con ese correo.";
    private static final String NO_ES_IMAGEN = "La foto debe ser una imagen JPG, PNG o WebP.";
    private static final String ID_INVALIDO = "El identificador de la ruta debe ser un número entero positivo.";
    private static final String PESO_INVALIDO = "El peso admite hasta 4 enteros y 3 decimales, por ejemplo 31.5 o 0.035.";
    private static final Map<Integer, String> CODIGOS = Map.of(
            400, "DATOS_INVALIDOS", 404, "RECURSO_NO_ENCONTRADO", 405, "METODO_NO_PERMITIDO",
            406, "RESPUESTA_NO_ACEPTABLE", 409, "CONFLICTO", 415, "CONTENIDO_NO_SOPORTADO");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configurarDependencias(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Cada prueba vuelve a aplicar las migraciones: datos de ejemplo intactos e identidades en 3 y 4.
        registry.add("spring.flyway.clean-disabled", () -> false);
        // Con barra final, para comprobar que la fotoUrl no queda con doble barra.
        registry.add("petvet.fotos.url-base", () -> URL_BASE_FOTOS + "/");
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
        assertError(consultar("POST", "/api/duenos/2/mascotas", mascota("Sol", "AVE", hoy, "0.0355")), 400,
                PESO_INVALIDO);
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
    void guardaElPesoConTresDecimalesSinRedondearlo() throws Exception {
        LocalDate nacimiento = LocalDate.of(2025, 3, 1);
        // Un periquito de 35 g, como lo envía el formulario del front.
        var pio = consultar("POST", "/api/duenos/2/mascotas", mascota("Pío", "AVE", nacimiento, "0.035"));
        assertThat(pio.estado()).isEqualTo(201);
        assertThat(pio.texto()).contains("\"peso\":0.035,");
        // La respuesta del POST sale del cuerpo enviado; el GET relee la base, que no debe redondear a 0.04.
        assertThat(consultar("GET", "/api/mascotas/4", null).texto()).contains("\"peso\":0.035,");
        assertThat(jdbc.queryForObject("SELECT peso FROM mascotas WHERE id = 4", BigDecimal.class))
                .isEqualByComparingTo("0.035");

        var editada = consultar("PUT", "/api/mascotas/4", mascota("Pío", "AVE", nacimiento, "0.005"));
        assertThat(editada.estado()).isEqualTo(200);
        assertThat(consultar("GET", "/api/mascotas/4", null).texto()).contains("\"peso\":0.005,");

        // Con un cuarto decimal el peso sigue siendo mayor que 0: solo se informa el límite de decimales.
        assertError(consultar("PUT", "/api/mascotas/4", mascota("Pío", "AVE", nacimiento, "0.0005")), 400,
                PESO_INVALIDO);
        assertError(consultar("PUT", "/api/mascotas/4", mascota("Pío", "AVE", nacimiento, "10000")), 400,
                PESO_INVALIDO);
        assertError(consultar("PUT", "/api/mascotas/4", mascota("Pío", "AVE", nacimiento, "-0.005")), 400,
                "El peso debe ser mayor que 0.");

        // Los datos de ejemplo conservan el peso de los mocks del front, sin ceros sobrantes.
        assertThat(consultar("GET", "/api/mascotas/1", null).texto()).contains("\"peso\":31.5,");
        assertThat(consultar("GET", "/api/mascotas/2", null).texto()).contains("\"peso\":4.2,");
        assertThat(consultar("GET", "/api/mascotas/3", null).texto()).contains("\"peso\":0.04,");
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
    void subirUnaFotoDevuelveLaUrlAbsolutaYElGetEntregaLosMismosBytes() throws Exception {
        byte[] png = imagen("png");
        var subida = subirFoto(3, "foto", "image/png", png);
        assertThat(subida.estado()).isEqualTo(200);
        String fotoUrl = subida.cuerpo().path("fotoUrl").asText();
        assertThat(fotoUrl).matches("http://mascotas\\.prueba/api/mascotas/3/foto\\?v=\\d+");
        assertThat(subida.cuerpo().path("nombre").asText()).isEqualTo("Kiwi");
        assertThat(subida.cuerpo().path("duenoId").asLong()).isEqualTo(2);

        var descarga = descargar(rutaDe(fotoUrl));
        assertThat(descarga.statusCode()).isEqualTo(200);
        assertThat(descarga.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(descarga.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(descarga.body()).isEqualTo(png);
        assertThat(consultar("GET", "/api/mascotas/3", null).cuerpo().path("fotoUrl").asText()).isEqualTo(fotoUrl);

        byte[] jpg = imagen("jpg");
        var reemplazo = subirFoto(3, "foto", "image/jpeg", jpg);
        assertThat(reemplazo.estado()).isEqualTo(200);
        String nuevaUrl = reemplazo.cuerpo().path("fotoUrl").asText();
        var nuevaDescarga = descargar(rutaDe(nuevaUrl));
        assertThat(nuevaDescarga.headers().firstValue("Content-Type")).hasValue("image/jpeg");
        assertThat(nuevaDescarga.body()).isEqualTo(jpg);

        var editada = consultar("PUT", "/api/mascotas/3", mascota("Kiwi", "AVE", LocalDate.of(2024, 2, 11), "0.05"));
        assertThat(editada.cuerpo().path("fotoUrl").asText()).isEqualTo(nuevaUrl);
        assertThat(editada.cuerpo().path("duenoId").asLong()).isEqualTo(2);

        // La base guarda bytes, tipo y versión; nunca la URL absoluta.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascotas WHERE foto_url LIKE 'http%'", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT tipo_contenido FROM fotos_mascotas WHERE mascota_id = 3", String.class))
                .isEqualTo("image/jpeg");
    }

    @Test
    void rechazaFotosQueNoSonImagenesPesanMasDe5MBONoVienenEnElCampoFoto() throws Exception {
        byte[] png = imagen("png");
        assertError(subirFoto(1, "foto", "text/plain", "hola".getBytes(StandardCharsets.UTF_8)), 400, NO_ES_IMAGEN);
        assertError(subirFoto(1, "foto", "image/png", "no soy un png".getBytes(StandardCharsets.UTF_8)), 400,
                NO_ES_IMAGEN);
        assertError(subirFoto(1, "foto", "image/gif", png), 400, NO_ES_IMAGEN);
        assertError(subirFoto(1, "imagen", "image/png", png), 400, "Adjunta la imagen en el campo foto del formulario.");
        // Por encima del límite del archivo y por debajo del de la solicitud (6 MB).
        assertError(subirFoto(1, "foto", "image/png", Arrays.copyOf(png, 5 * 1024 * 1024 + 1024)), 400,
                "La foto pesa más de 5 MB.");
        assertError(subirFoto(999, "foto", "image/png", png), 404, "No existe la mascota 999");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM fotos_mascotas", Long.class)).isZero();
        assertError(consultar("GET", "/api/mascotas/1/foto", null), 404, "No hay una foto subida para la mascota 1");
        assertThat(consultar("GET", "/api/mascotas/1", null).cuerpo().path("fotoUrl").asText())
                .isEqualTo("/mascotas/tobby.jpg");
    }

    @Test
    void laFotoDeMasDe5MBLlegaAlFrontComo400ConCorsYSinCortarLaConexion() throws Exception {
        byte[] png = imagen("png");
        // Sin CORS el navegador oculta el 400 y axios lo reporta como error de red. Con 7 MB también se pasa el
        // límite de la solicitud (6 MB) y quedan más de 2 MB sin leer, lo que antes cortaba la conexión.
        for (int tamano : List.of(5 * 1024 * 1024 + 1024, 7 * 1024 * 1024)) {
            var rechazada = enviar(solicitudDeFoto(1, "foto", "image/png", Arrays.copyOf(png, tamano))
                    .header("Origin", ORIGEN_FRONT));
            assertError(rechazada, 400, "La foto pesa más de 5 MB.");
            assertThat(rechazada.cabeceras().firstValue("Access-Control-Allow-Origin")).hasValue(ORIGEN_FRONT);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM fotos_mascotas", Long.class)).isZero();

        var subida = enviar(solicitudDeFoto(1, "foto", "image/png", png).header("Origin", ORIGEN_FRONT));
        assertThat(subida.estado()).isEqualTo(200);
        assertThat(subida.cabeceras().firstValue("Access-Control-Allow-Origin")).hasValue(ORIGEN_FRONT);
        assertThat(subida.cuerpo().path("fotoUrl").asText())
                .matches("http://mascotas\\.prueba/api/mascotas/1/foto\\?v=\\d+");
        assertThat(descargar(rutaDe(subida.cuerpo().path("fotoUrl").asText())).body()).isEqualTo(png);
    }

    @Test
    void subidasSimultaneasALaMismaMascotaSeAtiendenEnOrdenYCadaUnaCambiaLaUrl() throws Exception {
        byte[] png = imagen("png");
        byte[] jpg = imagen("jpg");
        var iniciar = new CountDownLatch(1);
        List<Respuesta> respuestas = new ArrayList<>();
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Respuesta>> envios = new ArrayList<>();
            for (int intento = 0; intento < 4; intento++) {
                boolean enPng = intento % 2 == 0;
                Callable<Respuesta> envio = () -> {
                    iniciar.await();
                    return enPng ? subirFoto(2, "foto", "image/png", png) : subirFoto(2, "foto", "image/jpeg", jpg);
                };
                envios.add(ejecutor.submit(envio));
            }
            iniciar.countDown();
            for (var envio : envios) {
                respuestas.add(envio.get());
            }
        }

        assertThat(respuestas).allSatisfy(respuesta -> assertThat(respuesta.estado()).isEqualTo(200));
        assertThat(respuestas).extracting(respuesta -> respuesta.cuerpo().path("fotoUrl").asText())
                .doesNotHaveDuplicates();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM fotos_mascotas WHERE mascota_id = 2", Long.class))
                .isEqualTo(1);
        var descarga = descargar(rutaDe(consultar("GET", "/api/mascotas/2", null).cuerpo().path("fotoUrl").asText()));
        boolean quedoPng = descarga.headers().firstValue("Content-Type").orElseThrow().equals("image/png");
        assertThat(descarga.body()).isEqualTo(quedoPng ? png : jpg);
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
        assertThat(documentacion.cuerpo().path("paths").has("/api/mascotas/{id}/foto")).isTrue();
    }

    @Test
    void metodosYTiposDeContenidoNoAdmitidosConservanSuEstadoHttp() throws Exception {
        assertError(consultar("DELETE", "/api/mascotas/1", null), 405, "El método HTTP no está disponible en esta ruta.");
        String noSoportado = "Envíe los datos como application/json, o la foto como multipart/form-data.";
        assertError(enviar(HttpRequest.newBuilder(uri("/api/duenos"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("contenido de texto"))), 415, noSoportado);
        assertError(consultar("POST", "/api/mascotas/1/foto", Map.of("foto", "tobby.jpg")), 415, noSoportado);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM duenos", Long.class)).isEqualTo(2);
    }

    @Test
    void losErroresSalenEnJsonAunqueElAcceptPidaOtroFormato() throws Exception {
        // Si el error se negociara con el Accept, el 404 terminaría en un 500 con la página Whitelabel.
        assertError(consultarAceptando("/api/mascotas/999", "text/html"), 404, "No existe la mascota 999");
        assertError(consultarAceptando("/api/mascotas/1/foto", "text/html"), 404,
                "No hay una foto subida para la mascota 1");
        assertError(consultarAceptando("/api/mascotas/1/foto", "image/*"), 404,
                "No hay una foto subida para la mascota 1");
        // El 406 trae el cuerpo de error que documenta el contrato, no una respuesta vacía.
        assertError(consultarAceptando("/api/mascotas/1", "application/xml"), 406,
                "Solicite la respuesta como application/json.");
    }

    private static Map<String, Object> mascota(String nombre, String especie, LocalDate fechaNacimiento, String peso) {
        return Map.of("nombre", nombre, "especie", especie, "raza", "Criollo",
                "fechaNacimiento", fechaNacimiento.toString(), "peso", new BigDecimal(peso));
    }

    private static byte[] imagen(String formato) throws Exception {
        var imagen = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        imagen.setRGB(3, 3, 0xF5A623);
        var salida = new ByteArrayOutputStream();
        assertThat(ImageIO.write(imagen, formato, salida)).isTrue();
        return salida.toByteArray();
    }

    /** La fotoUrl apunta a la base configurada; la prueba pide la misma ruta al puerto aleatorio. */
    private static String rutaDe(String fotoUrl) {
        URI url = URI.create(fotoUrl);
        return url.getRawPath() + "?" + url.getRawQuery();
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

    private Respuesta subirFoto(long mascotaId, String campo, String tipo, byte[] contenido) throws Exception {
        return enviar(solicitudDeFoto(mascotaId, campo, tipo, contenido));
    }

    private HttpRequest.Builder solicitudDeFoto(long mascotaId, String campo, String tipo, byte[] contenido) {
        String limite = "limite-petvet-" + System.nanoTime();
        var cuerpo = new ByteArrayOutputStream();
        cuerpo.writeBytes(("--" + limite + "\r\n"
                + "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"foto\"\r\n"
                + "Content-Type: " + tipo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.writeBytes(contenido);
        cuerpo.writeBytes(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(uri("/api/mascotas/" + mascotaId + "/foto"))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()));
    }

    private HttpResponse<byte[]> descargar(String ruta) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(ruta))
                .timeout(Duration.ofSeconds(10))
                // El mismo Accept que envía el navegador al cargar una etiqueta img.
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private Respuesta enviar(HttpRequest.Builder request) throws Exception {
        var response = http.send(request.timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
        // Solo se lee como JSON lo que llega como JSON: una página HTML o un cuerpo vacío fallan en la aserción.
        boolean esJson = response.headers().firstValue("Content-Type").orElse("").contains("json");
        return new Respuesta(response.statusCode(), response.body(),
                esJson ? objectMapper.readTree(response.body()) : MissingNode.getInstance(), response.headers());
    }

    private Respuesta consultarAceptando(String ruta, String accept) throws Exception {
        return enviar(HttpRequest.newBuilder(uri(ruta)).header("Accept", accept).GET());
    }

    private void assertError(Respuesta respuesta, int estado, String detalle) {
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(respuesta.cabeceras().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(respuesta.cuerpo().size()).isEqualTo(2);
        assertThat(respuesta.cuerpo().path("error").asText()).isEqualTo(CODIGOS.get(estado));
        assertThat(respuesta.cuerpo().path("detalle").asText()).isEqualTo(detalle);
    }

    private record Respuesta(int estado, String texto, JsonNode cuerpo, HttpHeaders cabeceras) {
        String ubicacion() {
            return cabeceras.firstValue("Location").orElse("");
        }
    }
}
