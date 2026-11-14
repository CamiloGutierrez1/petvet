package com.petvet.mascotas.presentation;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.petvet.mascotas.domain.ConflictoException;
import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoException error) {
        return respuesta(404, "RECURSO_NO_ENCONTRADO", error.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorApi> rutaNoEncontrada() {
        return respuesta(404, "RECURSO_NO_ENCONTRADO", "La ruta no existe.");
    }

    /** Incluye el correo repetido, tanto el que detecta el registro como el que rechaza la restricción UNIQUE. */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorApi> conflicto(ConflictoException error) {
        return respuesta(409, "CONFLICTO", error.getMessage());
    }

    // El correo repetido ya llega como ConflictoException. Cualquier otra violación de la base no se
    // presenta como un correo existente: indica una validación que falta y queda en el registro.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException error) {
        LOG.warn("La base de Mascotas y Dueños rechazó una operación", error);
        return respuesta(409, "CONFLICTO", "Los datos entran en conflicto con la información guardada.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> argumento(IllegalArgumentException error) {
        return respuesta(400, "DATOS_INVALIDOS", error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException error) {
        String detalle = error.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(campo -> String.valueOf(campo.getDefaultMessage())))
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return respuesta(400, "DATOS_INVALIDOS", detalle.isBlank() ? "Revise los campos enviados." : detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException error) {
        if (error.getCause() instanceof InvalidFormatException formato && formato.getTargetType() == Especie.class) {
            return respuesta(400, "DATOS_INVALIDOS", "La especie debe ser PERRO, GATO, AVE u OTRO.");
        }
        return respuesta(400, "DATOS_INVALIDOS",
                "Revise el JSON enviado: la fecha va en formato ISO, por ejemplo 2021-04-18, y el peso es un número.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ErrorApi> identificador() {
        return respuesta(400, "DATOS_INVALIDOS", "El identificador de la ruta debe ser un número entero positivo.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception error) {
        LOG.error("Falló una operación de Mascotas y Dueños", error);
        return respuesta(500, "ERROR_INTERNO", "No fue posible completar la operación.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> metodo() {
        return respuesta(405, "METODO_NO_PERMITIDO", "El método HTTP no está disponible en esta ruta.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorApi> contenido() {
        return respuesta(415, "CONTENIDO_NO_SOPORTADO", "Envíe los datos como application/json.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorApi> respuestaNoAceptable() {
        return respuesta(406, "RESPUESTA_NO_ACEPTABLE", "Solicite la respuesta como application/json.");
    }

    private ResponseEntity<ErrorApi> respuesta(int status, String error, String detalle) {
        return ResponseEntity.status(status).body(new ErrorApi(error, detalle));
    }

    public record ErrorApi(String error, String detalle) { }
}
