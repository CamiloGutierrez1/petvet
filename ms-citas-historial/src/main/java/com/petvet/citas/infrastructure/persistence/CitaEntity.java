package com.petvet.citas.infrastructure.persistence;

import com.petvet.citas.domain.Cita;
import com.petvet.citas.domain.EstadoCita;
import com.petvet.citas.domain.TipoServicio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class CitaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mascota_id", nullable = false)
    private long mascotaId;

    @Column(name = "veterinario_id", nullable = false)
    private long veterinarioId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio", nullable = false, length = 32)
    private TipoServicio tipoServicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoCita estado;

    protected CitaEntity() {
    }

    static CitaEntity fromDomain(Cita cita) {
        CitaEntity entity = new CitaEntity();
        entity.id = cita.id();
        entity.mascotaId = cita.mascotaId();
        entity.veterinarioId = cita.veterinarioId();
        entity.fechaHora = cita.fechaHora();
        entity.duracionMinutos = cita.duracionMinutos();
        entity.tipoServicio = cita.tipoServicio();
        entity.estado = cita.estado();
        return entity;
    }

    Cita toDomain() {
        return new Cita(id, mascotaId, veterinarioId, fechaHora, tipoServicio, estado, duracionMinutos);
    }
}
