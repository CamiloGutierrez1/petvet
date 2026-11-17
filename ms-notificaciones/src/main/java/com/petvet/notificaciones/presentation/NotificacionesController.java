package com.petvet.notificaciones.presentation;

import com.petvet.notificaciones.application.ConsultarNotificacionesUseCase;
import com.petvet.notificaciones.domain.Notificacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Notificaciones", description = "Correos que PetVet envió a partir de los eventos de Citas")
public class NotificacionesController {
    private final ConsultarNotificacionesUseCase consultar;

    public NotificacionesController(ConsultarNotificacionesUseCase consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/notificaciones")
    @Operation(summary = "Consultar las últimas notificaciones enviadas", description = "De la más reciente a la "
            + "más antigua. Cada una trae el messageId del mensaje de RabbitMQ: un mensaje repetido no genera otra.")
    public List<Notificacion> recientes(@RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El límite debe estar entre 1 y 100")
            @Max(value = ConsultarNotificacionesUseCase.LIMITE_MAXIMO, message = "El límite debe estar entre 1 y 100")
            int limite) {
        return consultar.recientes(limite);
    }
}
