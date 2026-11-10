package com.petvet.citas.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Construye una consulta con campos opcionales y valida el resultado al finalizar. */
public final class RegistroHistorialBuilder {

    private final long citaId;
    private final long mascotaId;
    private final long veterinarioId;
    private final LocalDateTime fecha;
    private TipoConsulta tipoConsulta = TipoConsulta.GENERAL;
    private String diagnostico;
    private List<String> vacunasAplicadas = List.of();
    private List<String> tratamientos = List.of();
    private List<String> examenes = List.of();
    private String observaciones;
    private LocalDate proximoControl;

    public RegistroHistorialBuilder(long citaId, long mascotaId, long veterinarioId, LocalDateTime fecha) {
        this.citaId = citaId;
        this.mascotaId = mascotaId;
        this.veterinarioId = veterinarioId;
        this.fecha = fecha;
    }

    public RegistroHistorialBuilder tipoConsulta(TipoConsulta tipoConsulta) {
        this.tipoConsulta = tipoConsulta;
        return this;
    }

    public RegistroHistorialBuilder diagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
        return this;
    }

    public RegistroHistorialBuilder vacunasAplicadas(List<String> vacunasAplicadas) {
        this.vacunasAplicadas = copiarLista(vacunasAplicadas);
        return this;
    }

    public RegistroHistorialBuilder tratamientos(List<String> tratamientos) {
        this.tratamientos = copiarLista(tratamientos);
        return this;
    }

    public RegistroHistorialBuilder examenes(List<String> examenes) {
        this.examenes = copiarLista(examenes);
        return this;
    }

    public RegistroHistorialBuilder observaciones(String observaciones) {
        this.observaciones = observaciones;
        return this;
    }

    public RegistroHistorialBuilder proximoControl(LocalDate proximoControl) {
        this.proximoControl = proximoControl;
        return this;
    }

    public RegistroHistorial build() {
        return new RegistroHistorial(null, citaId, mascotaId, veterinarioId, fecha, tipoConsulta,
                diagnostico, vacunasAplicadas, tratamientos, examenes, observaciones, proximoControl);
    }

    private static List<String> copiarLista(List<String> valores) {
        return valores == null ? List.of() : new ArrayList<>(valores);
    }
}
