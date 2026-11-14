package com.petvet.notificaciones.infrastructure.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petvet.notificaciones.domain.EnvioNoDisponibleException;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class CorreoSmtpAdapterTest {

    private static final String CUERPO = "Hola, Laura Méndez:\n\nLa cita de vacunación de Tobby quedó lista.\n";

    @Mock private JavaMailSender mailSender;
    private CorreoSmtpAdapter adapter;

    @BeforeEach
    void crearAdapter() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        adapter = new CorreoSmtpAdapter(mailSender, "PetVet <notificaciones@petvet.local>");
    }

    @Test
    void armaUnCorreoDeTextoEnUtf8ConElRemitenteConfigurado() throws Exception {
        adapter.enviar("laura.mendez@correo.com", "Confirmación de la cita de Tobby", CUERPO);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviado.capture());
        MimeMessage correo = enviado.getValue();
        InternetAddress remitente = (InternetAddress) correo.getFrom()[0];
        assertThat(remitente.getPersonal()).isEqualTo("PetVet");
        assertThat(remitente.getAddress()).isEqualTo("notificaciones@petvet.local");
        assertThat(correo.getRecipients(Message.RecipientType.TO)).extracting(Address::toString)
                .containsExactly("laura.mendez@correo.com");
        assertThat(correo.getSubject()).isEqualTo("Confirmación de la cita de Tobby");
        assertThat(correo.getHeader("Subject")[0]).startsWith("=?UTF-8?");
        assertThat(correo.getContent()).isEqualTo(CUERPO);
        assertThat(correo.getDataHandler().getContentType()).isEqualTo("text/plain; charset=UTF-8");
    }

    @Test
    void traduceLaFallaDelSmtpAUnaExcepcionDelDominio() {
        MailSendException falla = new MailSendException("Connection refused");
        doThrow(falla).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> adapter.enviar("laura.mendez@correo.com", "Asunto", CUERPO))
                .isInstanceOf(EnvioNoDisponibleException.class)
                .hasMessage("No fue posible enviar el correo por SMTP")
                .hasCause(falla);
    }
}
