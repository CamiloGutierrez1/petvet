package com.petvet.notificaciones.infrastructure.persistence;

import com.petvet.notificaciones.domain.Notificacion;
import com.petvet.notificaciones.domain.TipoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Lectura de las notificaciones enviadas. La escritura usa un INSERT nativo con ON CONFLICT para
 * deduplicar sin provocar un error de unicidad que dañe la transacción.
 */
@Entity
@Table(name = "notificaciones")
public class NotificacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoNotificacion tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String destino;

    @Column(nullable = false, columnDefinition = "text")
    private String asunto;

    @Column(nullable = false, columnDefinition = "text")
    private String cuerpo;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "enviada_en", nullable = false)
    private Instant enviadaEn;

    protected NotificacionEntity() {
    }

    Notificacion toDomain() {
        return new Notificacion(id, messageId, tipo, destino, asunto, cuerpo, creadaEn, enviadaEn);
    }
}
