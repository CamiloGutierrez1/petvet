package com.petvet.citas.infrastructure;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petvet.citas.application.VacunasService;
import com.petvet.citas.domain.CitaRepository;
import com.petvet.citas.domain.DependenciaNoDisponibleException;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordatoriosVacunasJobTest {

    @Mock private CitaRepository citas;
    @Mock private VacunasService vacunas;
    private RecordatoriosVacunasJob job;

    @BeforeEach
    void prepararTarea() {
        job = new RecordatoriosVacunasJob(citas, vacunas);
        when(citas.mascotasConCitas()).thenReturn(List.of(1L, 2L, 3L));
    }

    @Test
    void unaMascotaConErrorNoImpideRevisarLasDemas() {
        doThrow(new RecursoNoEncontradoException("No existe la mascota 2"))
                .when(vacunas).publicarRecordatorios(2L);

        job.ejecutar();

        verify(vacunas).publicarRecordatorios(1L);
        verify(vacunas).publicarRecordatorios(2L);
        verify(vacunas).publicarRecordatorios(3L);
    }

    @Test
    void siMascotasNoRespondeSeCortaLaEjecucionSinInsistirConCadaMascota() {
        doThrow(new DependenciaNoDisponibleException("Mascotas no disponible"))
                .when(vacunas).publicarRecordatorios(2L);

        job.ejecutar();

        verify(vacunas).publicarRecordatorios(1L);
        verify(vacunas).publicarRecordatorios(2L);
        verify(vacunas, never()).publicarRecordatorios(3L);
    }
}
