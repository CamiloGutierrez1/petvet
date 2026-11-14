package com.petvet.notificaciones.application.eventos;

import java.util.List;

/** Reglas comunes para leer los campos de los eventos. */
final class Campos {

    private Campos() {
    }

    /** Exige un valor presente; un texto vacío cuenta como ausente. */
    static <T> T obligatorio(T valor, String campo) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) {
            throw new IllegalArgumentException("El evento no trae el campo " + campo);
        }
        return valor;
    }

    /** Una lista ausente se trata como vacía: los eventos viejos no traen los campos nuevos. */
    static <T> List<T> lista(List<T> valores) {
        return valores == null ? List.of() : List.copyOf(valores);
    }

    /** Un texto opcional vacío se trata como ausente. */
    static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
