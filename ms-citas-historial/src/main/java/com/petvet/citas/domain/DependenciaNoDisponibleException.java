package com.petvet.citas.domain;

public class DependenciaNoDisponibleException extends RuntimeException {
    public DependenciaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
