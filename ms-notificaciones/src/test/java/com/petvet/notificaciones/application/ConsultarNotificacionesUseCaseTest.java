package com.petvet.notificaciones.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petvet.notificaciones.domain.Notificacion;
import com.petvet.notificaciones.domain.NotificacionRepository;
import com.petvet.notificaciones.domain.TipoNotificacion;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarNotificacionesUseCaseTest {

    @Mock private NotificacionRepository notificaciones;
    @InjectMocks private ConsultarNotificacionesUseCase consultar;

    @Test
    void devuelveLasUltimasNotificacionesConElLimitePedido() {
        Notificacion enviada = new Notificacion(1L, "m-1", TipoNotificacion.CITA_AGENDADA, "laura.mendez@correo.com",
                "Confirmación de la cita de Tobby", "Hola", Instant.EPOCH, Instant.EPOCH);
        when(notificaciones.recientes(20)).thenReturn(List.of(enviada));

        assertThat(consultar.recientes(20)).containsExactly(enviada);
    }

    @Test
    void rechazaUnLimiteFueraDeRango() {
        assertThatThrownBy(() -> consultar.recientes(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El límite debe estar entre 1 y 100");
        assertThatThrownBy(() -> consultar.recientes(101)).isInstanceOf(IllegalArgumentException.class);
        verify(notificaciones, never()).recientes(anyInt());
    }
}
