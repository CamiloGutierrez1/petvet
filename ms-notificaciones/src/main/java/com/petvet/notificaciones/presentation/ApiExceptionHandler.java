package com.petvet.notificaciones.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Errores con el mismo formato que Citas: {"error": CODIGO, "detalle": texto}. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorApi> validacion(HandlerMethodValidationException error) {
        String detalle = error.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct().sorted()
                .reduce((a, b) -> a + ". " + b).orElse("Revise los parámetros enviados");
        return respuesta(400, "DATOS_INVALIDOS", detalle);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> argumento(IllegalArgumentException error) {
        return respuesta(400, "DATOS_INVALIDOS", error.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> formato() {
        return respuesta(400, "DATOS_INVALIDOS", "Revise los parámetros enviados");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorApi> rutaNoEncontrada() {
        return respuesta(404, "RECURSO_NO_ENCONTRADO", "La ruta no existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> metodo() {
        return respuesta(405, "METODO_NO_PERMITIDO", "El método HTTP no está disponible en esta ruta");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorApi> noAceptable() {
        return respuesta(406, "RESPUESTA_NO_ACEPTABLE", "Solicite la respuesta como application/json");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception error) {
        LOG.error("Falló una operación de Notificaciones", error);
        return respuesta(500, "ERROR_INTERNO", "No fue posible completar la operación");
    }

    private ResponseEntity<ErrorApi> respuesta(int status, String error, String detalle) {
        return ResponseEntity.status(status).body(new ErrorApi(error, detalle));
    }

    public record ErrorApi(String error, String detalle) { }
}
