package com.petvet.notificaciones.application;

import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.application.eventos.CitaCompletada;
import com.petvet.notificaciones.application.eventos.VacunaPendiente;
import com.petvet.notificaciones.application.eventos.VacunaProxima;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Textos en español de los tres correos, en texto plano y tratando al dueño de usted. Las fechas de
 * los eventos ya son locales de la clínica y se escriben sin convertir zona; el reloj solo decide si
 * una vacuna ya venció.
 */
@Component
public class PlantillasCorreo {
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ESPANOL);
    // Hora de 24 h: con "h:mm a", JDK 21 escribe "a. m." con un espacio especial (U+202F).
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ESPANOL);
    private static final Map<String, String> SERVICIOS = Map.of(
            "CONSULTA_GENERAL", "consulta general",
            "VACUNACION", "vacunación",
            "CONTROL", "control",
            "ESPECIALISTA", "consulta con especialista");
    private static final String DESPEDIDA = "Equipo PetVet";

    private final Clock clock;

    public PlantillasCorreo(Clock clock) {
        this.clock = clock;
    }

    public MensajeCorreo citaAgendada(CitaAgendada cita) {
        String cuerpo = """
                %s

                La cita de %s de %s con %s quedó para el %s a las %s.

                Si no puede asistir, cancele la cita en PetVet para liberar el turno.

                %s
                """.formatted(saludo(cita.duenoNombre()), servicio(cita.tipoServicio()), cita.mascotaNombre(),
                conArticulo(cita.veterinarioNombre()), fecha(cita.fechaHora().toLocalDate()),
                HORA.format(cita.fechaHora()), DESPEDIDA);
        return new MensajeCorreo("Confirmación de la cita de " + cita.mascotaNombre(), cuerpo);
    }

    public MensajeCorreo citaCompletada(CitaCompletada cita) {
        List<String> lineas = new ArrayList<>();
        lineas.add(saludo(cita.duenoNombre()));
        lineas.add("");
        lineas.add("Este es el resumen de la consulta de " + cita.mascotaNombre() + ".");
        lineas.add("");
        lineas.add("Diagnóstico: " + cita.diagnostico());
        lineas.add("Vacunas aplicadas: " + (cita.vacunasAplicadas().isEmpty()
                ? "ninguna" : String.join(", ", cita.vacunasAplicadas())));
        if (!cita.examenes().isEmpty()) {
            // Citas guarda los nombres de los exámenes, no sus resultados.
            lineas.add("Exámenes registrados en la consulta: " + String.join(", ", cita.examenes()));
        }
        lineas.add("Próximo control: " + (cita.proximoControl() == null
                ? "sin control programado" : fecha(cita.proximoControl())));
        lineas.add("");
        if (cita.proximasVacunas().isEmpty()) {
            lineas.add("Próximas vacunas: ninguna pendiente");
        } else {
            lineas.add("Próximas vacunas:");
            for (VacunaPendiente vacuna : cita.proximasVacunas()) {
                lineas.add("- " + vacuna.vacuna() + ": " + plazo(vacuna.fechaLimite()));
            }
        }
        lineas.add("");
        lineas.add(DESPEDIDA);
        return new MensajeCorreo("Resumen de la consulta de " + cita.mascotaNombre(),
                String.join("\n", lineas) + "\n");
    }

    public MensajeCorreo vacunaProxima(VacunaProxima aviso) {
        String vacuna = "La vacuna " + aviso.vacuna() + " de su mascota " + aviso.mascotaNombre();
        String limite = fecha(aviso.fechaLimite());
        String estado = switch (comparadaConHoy(aviso.fechaLimite())) {
            case -1 -> vacuna + " venció el " + limite + ".";
            case 0 -> vacuna + " vence hoy, " + limite + ".";
            default -> vacuna + " va a vencer pronto. Aplíquela antes del " + limite + ".";
        };
        String cuerpo = """
                %s

                %s

                Puede agendar la cita de vacunación en PetVet.

                %s
                """.formatted(saludo(aviso.duenoNombre()), estado, DESPEDIDA);
        return new MensajeCorreo("Recordatorio: vacuna " + aviso.vacuna() + " de " + aviso.mascotaNombre(), cuerpo);
    }

    /** Las próximas vacunas de una mascota sin historial pueden traer fechas ya vencidas. */
    private String plazo(LocalDate fechaLimite) {
        String limite = fecha(fechaLimite);
        return switch (comparadaConHoy(fechaLimite)) {
            case -1 -> "venció el " + limite;
            case 0 -> "vence hoy, " + limite;
            default -> "antes del " + limite;
        };
    }

    private int comparadaConHoy(LocalDate fecha) {
        return Integer.signum(fecha.compareTo(LocalDate.now(clock)));
    }

    private static String saludo(String duenoNombre) {
        return duenoNombre == null ? "Hola:" : "Hola, " + duenoNombre + ":";
    }

    private static String servicio(String tipoServicio) {
        return SERVICIOS.getOrDefault(tipoServicio, tipoServicio.replace('_', ' ').toLowerCase(ESPANOL));
    }

    /** El nombre del veterinario ya trae "Dra." o "Dr.": se antepone el artículo que corresponde. */
    private static String conArticulo(String veterinario) {
        if (veterinario.startsWith("Dra.")) {
            return "la " + veterinario;
        }
        if (veterinario.startsWith("Dr.")) {
            return "el " + veterinario;
        }
        return veterinario;
    }

    private static String fecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }
}
