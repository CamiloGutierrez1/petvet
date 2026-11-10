package com.petvet.citas.infrastructure.persistence;

import com.petvet.citas.domain.RegistroHistorial;
import com.petvet.citas.domain.TipoConsulta;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registros_historial")
public class RegistroHistorialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false, unique = true)
    private long citaId;

    @Column(name = "mascota_id", nullable = false)
    private long mascotaId;

    @Column(name = "veterinario_id", nullable = false)
    private long veterinarioId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_consulta", nullable = false, length = 16)
    private TipoConsulta tipoConsulta;

    @Column(nullable = false, columnDefinition = "text")
    private String diagnostico;

    @ElementCollection
    @CollectionTable(name = "historial_vacunas", joinColumns = @JoinColumn(name = "historial_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "valor", nullable = false, columnDefinition = "text")
    private List<String> vacunasAplicadas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "historial_tratamientos", joinColumns = @JoinColumn(name = "historial_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "valor", nullable = false, columnDefinition = "text")
    private List<String> tratamientos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "historial_examenes", joinColumns = @JoinColumn(name = "historial_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "valor", nullable = false, columnDefinition = "text")
    private List<String> examenes = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "proximo_control")
    private LocalDate proximoControl;

    protected RegistroHistorialEntity() {
    }

    static RegistroHistorialEntity fromDomain(RegistroHistorial registro) {
        RegistroHistorialEntity entity = new RegistroHistorialEntity();
        entity.id = registro.id();
        entity.citaId = registro.citaId();
        entity.mascotaId = registro.mascotaId();
        entity.veterinarioId = registro.veterinarioId();
        entity.fecha = registro.fecha();
        entity.tipoConsulta = registro.tipoConsulta();
        entity.diagnostico = registro.diagnostico();
        entity.vacunasAplicadas = new ArrayList<>(registro.vacunasAplicadas());
        entity.tratamientos = new ArrayList<>(registro.tratamientos());
        entity.examenes = new ArrayList<>(registro.examenes());
        entity.observaciones = registro.observaciones();
        entity.proximoControl = registro.proximoControl();
        return entity;
    }

    RegistroHistorial toDomain() {
        return new RegistroHistorial(id, citaId, mascotaId, veterinarioId, fecha, tipoConsulta,
                diagnostico, List.copyOf(vacunasAplicadas), List.copyOf(tratamientos),
                List.copyOf(examenes), observaciones, proximoControl);
    }
}
