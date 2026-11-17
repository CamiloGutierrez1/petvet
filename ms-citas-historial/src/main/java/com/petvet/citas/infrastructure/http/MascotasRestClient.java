package com.petvet.citas.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.petvet.citas.domain.DependenciaNoDisponibleException;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class MascotasRestClient implements MascotaClient {

    private static final String ERROR_RESPUESTA =
            "Mascotas y Dueños devolvió datos incompletos o inválidos";
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final RestClient restClient;
    private final Clock clock;

    public MascotasRestClient(RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    @Override
    public Mascota buscar(long id) {
        JsonNode respuesta;
        try {
            respuesta = restClient.get()
                    .uri("/api/mascotas/{id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new RecursoNoEncontradoException("No existe la mascota " + id);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new DependenciaNoDisponibleException(
                                "Mascotas y Dueños no pudo atender la consulta. Intenta nuevamente");
                    })
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new DependenciaNoDisponibleException(
                    "No se pudo consultar Mascotas y Dueños. Intenta nuevamente");
        }
        return convertir(respuesta, id);
    }

    private Mascota convertir(JsonNode respuesta, long id) {
        if (respuesta == null || !respuesta.isObject()) {
            throw new DependenciaNoDisponibleException(ERROR_RESPUESTA);
        }
        JsonNode identificador = respuesta.path("id");
        if (!identificador.isIntegralNumber() || !identificador.canConvertToLong()
                || identificador.longValue() != id) {
            throw new DependenciaNoDisponibleException(ERROR_RESPUESTA);
        }

        String nombre = texto(respuesta.path("nombre"));
        String especieTexto = texto(respuesta.path("especie"));
        String nacimientoTexto = texto(respuesta.path("fechaNacimiento"));
        String duenoNombre = datoDueno(respuesta, "duenoNombre", "nombre");
        String duenoEmail = datoDueno(respuesta, "duenoEmail", "email");
        if (!EMAIL.matcher(duenoEmail).matches()) {
            throw new DependenciaNoDisponibleException(ERROR_RESPUESTA);
        }

        Especie especie;
        LocalDate fechaNacimiento;
        try {
            especie = Especie.valueOf(especieTexto);
            fechaNacimiento = LocalDate.parse(nacimientoTexto);
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException exception) {
            throw new DependenciaNoDisponibleException(ERROR_RESPUESTA);
        }
        if (fechaNacimiento.isAfter(LocalDate.now(clock))) {
            throw new DependenciaNoDisponibleException(ERROR_RESPUESTA);
        }
        return new Mascota(id, nombre, especie, fechaNacimiento, duenoNombre, duenoEmail);
    }

    private String datoDueno(JsonNode respuesta, String campoPlano, String campoAnidado) {
        JsonNode valor = respuesta.path(campoPlano);
        if (valor.isMissingNode() || valor.isNull()) {
            valor = respuesta.path("dueno").path(campoAnidado);
        }
        return texto(valor);
    }

    private String texto(JsonNode valor) {
        if (!valor.isTextual() || valor.textValue().isBlank()) {
            throw new DependenciaNoDisponibleException(ERROR_RESPUESTA);
        }
        return valor.textValue().strip();
    }
}
