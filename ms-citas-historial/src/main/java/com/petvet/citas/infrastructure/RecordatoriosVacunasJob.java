package com.petvet.citas.infrastructure;

import com.petvet.citas.application.VacunasService;
import com.petvet.citas.domain.CitaRepository;
import com.petvet.citas.domain.DependenciaNoDisponibleException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "petvet.vacunas.habilitadas", havingValue = "true", matchIfMissing = true)
public class RecordatoriosVacunasJob {
    private static final Logger LOG = LoggerFactory.getLogger(RecordatoriosVacunasJob.class);
    private final CitaRepository citas;
    private final VacunasService vacunas;

    public RecordatoriosVacunasJob(CitaRepository citas, VacunasService vacunas) {
        this.citas = citas;
        this.vacunas = vacunas;
    }

    @Scheduled(cron = "${petvet.vacunas.cron}", zone = "${petvet.agenda.zona}")
    public void ejecutar() {
        List<Long> mascotas = citas.mascotasConCitas();
        for (int revisadas = 0; revisadas < mascotas.size(); revisadas++) {
            Long mascotaId = mascotas.get(revisadas);
            try {
                vacunas.publicarRecordatorios(mascotaId);
            } catch (DependenciaNoDisponibleException error) {
                // Sin Mascotas y Dueños fallaría cada mascota tras su tiempo de espera. Se corta la ejecución.
                LOG.warn("Mascotas y Dueños no responde. Quedan {} de {} mascotas para la siguiente ejecución",
                        mascotas.size() - revisadas, mascotas.size());
                return;
            } catch (RuntimeException error) {
                LOG.warn("No se pudo revisar la mascota {}. Se reintentará en la siguiente ejecución ({})",
                        mascotaId, error.getClass().getSimpleName());
            }
        }
    }
}
