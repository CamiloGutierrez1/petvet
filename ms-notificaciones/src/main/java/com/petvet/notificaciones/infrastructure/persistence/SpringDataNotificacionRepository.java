package com.petvet.notificaciones.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataNotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    /** Devuelve 1 si guardó la notificación y 0 si el messageId ya estaba registrado. */
    @Modifying
    @Query(value = """
            INSERT INTO notificaciones (message_id, tipo, destino, asunto, cuerpo, creada_en, enviada_en)
            VALUES (:messageId, :tipo, :destino, :asunto, :cuerpo, :creadaEn, :enviadaEn)
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int insertarSiEsNueva(@Param("messageId") String messageId, @Param("tipo") String tipo,
            @Param("destino") String destino, @Param("asunto") String asunto, @Param("cuerpo") String cuerpo,
            @Param("creadaEn") Instant creadaEn, @Param("enviadaEn") Instant enviadaEn);

    List<NotificacionEntity> findAllByOrderByEnviadaEnDescIdDesc(Limit limite);
}
