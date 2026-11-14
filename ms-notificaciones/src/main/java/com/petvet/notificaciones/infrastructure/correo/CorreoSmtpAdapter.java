package com.petvet.notificaciones.infrastructure.correo;

import com.petvet.notificaciones.domain.EnviadorNotificaciones;
import com.petvet.notificaciones.domain.EnvioNoDisponibleException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Adapter: implementa {@link EnviadorNotificaciones} con el JavaMailSender de Spring, que en local
 * apunta al SMTP de MailHog. Traduce los errores de correo a una excepción del dominio.
 */
@Component
public class CorreoSmtpAdapter implements EnviadorNotificaciones {
    private final JavaMailSender mailSender;
    private final String remitente;

    public CorreoSmtpAdapter(JavaMailSender mailSender,
            @Value("${petvet.notificaciones.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destino, String asunto, String cuerpo) {
        try {
            MimeMessage correo = mailSender.createMimeMessage();
            MimeMessageHelper datos = new MimeMessageHelper(correo, StandardCharsets.UTF_8.name());
            datos.setFrom(remitente);
            datos.setTo(destino);
            datos.setSubject(asunto);
            datos.setText(cuerpo);
            mailSender.send(correo);
        } catch (MailException | MessagingException error) {
            throw new EnvioNoDisponibleException("No fue posible enviar el correo por SMTP", error);
        }
    }
}
