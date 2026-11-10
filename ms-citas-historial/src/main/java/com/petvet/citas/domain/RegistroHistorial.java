package com.petvet.citas.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record RegistroHistorial(Long id, long citaId, long mascotaId, long veterinarioId,
                                LocalDateTime fecha, TipoConsulta tipoConsulta, String diagnostico,
                                List<String> vacunasAplicadas, List<String> tratamientos,
                                List<String> examenes, String observaciones, LocalDate proximoControl) {

    public RegistroHistorial {
        if (citaId <= 0 || mascotaId <= 0 || veterinarioId <= 0) {
            throw new IllegalArgumentException("Los identificadores del historial deben ser positivos");
        }
        Objects.requireNonNull(fecha, "La fecha de la consulta es obligatoria");
        Objects.requireNonNull(tipoConsulta, "El tipo de consulta es obligatorio");
        if (diagnostico == null || diagnostico.isBlank()) {
            throw new IllegalArgumentException("El diagnóstico es obligatorio");
        }
        if (proximoControl != null && proximoControl.isBefore(fecha.toLocalDate())) {
            throw new IllegalArgumentException("El próximo control debe ser igual o posterior a la consulta");
        }
        diagnostico = diagnostico.strip();
        observaciones = observaciones == null || observaciones.isBlank() ? null : observaciones.strip();
        vacunasAplicadas = copiarLista(vacunasAplicadas);
        tratamientos = copiarLista(tratamientos);
        examenes = copiarLista(examenes);
    }

    private static List<String> copiarLista(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream().map(valor -> {
            if (valor == null || valor.isBlank()) {
                throw new IllegalArgumentException("Los elementos del historial deben contener texto");
            }
            return valor.strip();
        }).toList();
    }
}
