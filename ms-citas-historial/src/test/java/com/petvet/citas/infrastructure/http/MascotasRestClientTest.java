package com.petvet.citas.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.petvet.citas.domain.DependenciaNoDisponibleException;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MascotasRestClientTest {

    private static final String RESPUESTA = """
            {"id":3,"nombre":"Luna","especie":"PERRO","fechaNacimiento":"2020-02-01",
             "duenoNombre":"Dueño Demo","duenoEmail":"demo@example.test"}
            """;

    private MockRestServiceServer server;
    private MascotasRestClient client;

    @BeforeEach
    void configurar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://mascotas.test");
        server = MockRestServiceServer.bindTo(builder).build();
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T02:00:00Z"), ZoneId.of("America/Bogota"));
        client = new MascotasRestClient(builder.build(), clock);
    }

    @AfterEach
    void verificarPeticiones() {
        server.verify();
    }

    @Test
    void consultaElContratoPlano() {
        responder(RESPUESTA);

        Mascota mascota = client.buscar(3);

        assertThat(mascota).isEqualTo(new Mascota(3L, "Luna", Especie.PERRO,
                LocalDate.of(2020, 2, 1), "Dueño Demo", "demo@example.test"));
    }

    @Test
    void aceptaLosDatosDelDuenoAnidados() {
        responder("""
                {"id":3,"nombre":"Luna","especie":"GATO","fechaNacimiento":"2020-02-01",
                 "dueno":{"nombre":"Dueño Demo","email":"demo@example.test"}}
                """);

        assertThat(client.buscar(3).duenoEmail()).isEqualTo("demo@example.test");
    }

    @Test
    void traduceEl404AMascotaInexistente() {
        server.expect(requestTo("http://mascotas.test/api/mascotas/3"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe la mascota 3");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void traduceLosErroresDelServicioADependenciaNoDisponible(int status) {
        server.expect(requestTo("http://mascotas.test/api/mascotas/3"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void traduceUnTimeoutSinExponerElDetalleDeRed() {
        server.expect(requestTo("http://mascotas.test/api/mascotas/3"))
                .andRespond(withException(new SocketTimeoutException("private upstream address")));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class)
                .hasMessageNotContaining("private upstream address");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[]", "{", "\"texto\""})
    void rechazaUnCuerpoInvalido(String respuesta) {
        responder(respuesta);

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void rechazaUnaRespuestaSinCuerpo() {
        server.expect(requestTo("http://mascotas.test/api/mascotas/3"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4", "3.5", "9223372036854775808", "\"3\""})
    void rechazaUnIdentificadorDistintoOInvalido(String id) {
        responder(RESPUESTA.replace("\"id\":3", "\"id\":" + id));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"desconocido", "perro", ""})
    void rechazaUnaEspecieDesconocida(String especie) {
        responder(RESPUESTA.replace("PERRO", especie));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-fecha", "2026-11-10", "2020-13-01"})
    void rechazaNacimientoInvalidoOFuturoEnBogota(String fecha) {
        responder(RESPUESTA.replace("2020-02-01", fecha));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void admiteNacimientoHoyEnBogota() {
        responder(RESPUESTA.replace("2020-02-01", "2026-11-09"));

        assertThat(client.buscar(3).fechaNacimiento()).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "demo", "demo@example", "demo @example.test"})
    void rechazaElCorreoInvalidoDelDueno(String correo) {
        responder(RESPUESTA.replace("demo@example.test", correo));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void rechazaElNombreVacio() {
        responder(RESPUESTA.replace("Luna", "   "));

        assertThatThrownBy(() -> client.buscar(3))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    private void responder(String json) {
        server.expect(requestTo("http://mascotas.test/api/mascotas/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
