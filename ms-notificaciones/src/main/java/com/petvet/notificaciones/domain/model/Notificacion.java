package com.petvet.notificaciones.domain.model;

import java.time.LocalDateTime;

public class Notificacion {

    private Long id;
    private String correoDueno;
    private String asunto;
    private String mensaje;
    private LocalDateTime fechaCreacion;
    private String messageId;

    public Notificacion(
            String correoDueno,
            String asunto,
            String mensaje,
            String messageId
    ) {
        this.correoDueno = correoDueno;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.messageId = messageId;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCorreoDueno() {
        return correoDueno;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getMessageId() {
        return messageId;
    }
}
