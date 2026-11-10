package com.petvet.citas.presentation;

import com.petvet.citas.domain.ConflictoException;
import com.petvet.citas.domain.DependenciaNoDisponibleException;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
        return respuesta(404, "RECURSO_NO_ENCONTRADO", "La ruta no existe");
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorApi> conflicto(ConflictoException error) {
        return respuesta(409, "CONFLICTO", error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> integridad() {
        return respuesta(409, "CONFLICTO", "El turno ya está ocupado o la operación ya fue registrada");
    }

    // Interbloqueo o espera de bloqueo entre solicitudes simultáneas sobre el mismo turno o cita.
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorApi> concurrencia() {
        return respuesta(409, "CONFLICTO", "Otra operación modificó el mismo turno o cita. Intenta nuevamente");
    }

    @ExceptionHandler(DependenciaNoDisponibleException.class)
    public ResponseEntity<ErrorApi> dependencia(DependenciaNoDisponibleException error) {
        return respuesta(503, "DEPENDENCIA_NO_DISPONIBLE", error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> argumento(IllegalArgumentException error) {
        return respuesta(400, "DATOS_INVALIDOS", error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException error) {
        String detalle = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + " " + campo.getDefaultMessage()).sorted()
                .reduce((a, b) -> a + ". " + b).orElse("Revise los campos enviados");
        return respuesta(400, "DATOS_INVALIDOS", detalle);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorApi> formato() {
        return respuesta(400, "DATOS_INVALIDOS", "Revise el JSON, las fechas y los identificadores enviados");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception error) {
        LOG.error("Falló una operación de Citas e Historial", error);
        return respuesta(500, "ERROR_INTERNO", "No fue posible completar la operación");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> metodo() {
        return respuesta(405, "METODO_NO_PERMITIDO", "El método HTTP no está disponible en esta ruta");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorApi> contenido() {
        return respuesta(415, "CONTENIDO_NO_SOPORTADO", "Envíe el cuerpo con Content-Type application/json");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorApi> respuestaNoAceptable() {
        return respuesta(406, "RESPUESTA_NO_ACEPTABLE", "Solicite la respuesta como application/json");
    }

    private ResponseEntity<ErrorApi> respuesta(int status, String error, String detalle) {
        return ResponseEntity.status(status).body(new ErrorApi(error, detalle));
    }

    public record ErrorApi(String error, String detalle) { }
}
