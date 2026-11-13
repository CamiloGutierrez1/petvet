import java.time.LocalDate;

public class Notificacion {

    // 1. Recordatorio de cita médica
    public void notificarRecordatorioCita(
            String correoDueno,
            String nombreDueno,
            String especialidadMedica,
            String nombreDoctor,
            LocalDate fechaCita) {

        System.out.println("Enviando email a: " + correoDueno);

        System.out.println(
                "Señor@. " + nombreDueno
                        + ", has programado una cita de "
                        + especialidadMedica
                        + " con Doctor@. "
                        + nombreDoctor
                        + " para el día " + fechaCita + "."
        );
    }

    // 2. Alerta de vencimiento de vacuna
    public void notificarAlertaVacunaVencimiento(
            String correoDueno,
            String nombreDueno,
            String tipoVacuna,
            String nombreMascota) {

        System.out.println("Enviando email a: " + correoDueno);

        System.out.println(
                "Señor@." + nombreDueno
                        + ", la vacuna de " + tipoVacuna
                        + " de su mascota " + nombreMascota
                        + " va a vencer pronto. "
                        + "No olvides renovarla. ¡Gracias!"
        );
    }

    // 3. Disponibilidad de resultados de exámenes
    public void notificarDisponibilidadResultadoExamenes(
            String correoDueno,
            String nombreDueno,
            String tipoExamen,
            String nombreMascota) {

        System.out.println("Enviando email a: " + correoDueno);

        System.out.println(
                "Señor@. " + nombreDueno
                        + ", ya están disponibles los resultados de "
                        + tipoExamen
                        + " de su mascota " + nombreMascota
                        + ". Ya puedes revisar los reportes."
        );
    }
}
