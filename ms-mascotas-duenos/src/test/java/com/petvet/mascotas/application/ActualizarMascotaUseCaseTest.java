package com.petvet.mascotas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import com.petvet.mascotas.domain.RegistroMascotas;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActualizarMascotaUseCaseTest {

    private static final LocalDate NACIMIENTO = LocalDate.of(2025, 6, 1);
    private static final DatosMascota CAMBIOS = new DatosMascota(" Max ", Especie.PERRO, "Beagle", NACIMIENTO,
            new BigDecimal("12.5"));

    @Mock private ConsultaMascotas consulta;
    @Mock private RegistroMascotas registro;
    private ActualizarMascotaUseCase actualizar;

    @BeforeEach
    void prepararCasoDeUso() {
        actualizar = new ActualizarMascotaUseCase(consulta, registro,
                Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneId.of("America/Bogota")));
    }

    @Test
    void mascotaInexistenteRespondeNoEncontradoSinActualizar() {
        when(consulta.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actualizar.ejecutar(999L, CAMBIOS))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe la mascota 999");

        verifyNoInteractions(registro);
    }

    @Test
    void conservaLaFotoYElDuenoQueElCuerpoNoTrae() {
        Mascota actual = new Mascota(4L, "Maxi", Especie.PERRO, "Criollo", NACIMIENTO, new BigDecimal("11.00"),
                "http://localhost:8081/api/mascotas/4/foto?v=1", 2L);
        Mascota esperada = new Mascota(4L, "Max", Especie.PERRO, "Beagle", NACIMIENTO, new BigDecimal("12.5"),
                actual.fotoUrl(), 2L);
        when(consulta.buscarPorId(4L)).thenReturn(Optional.of(actual));
        when(registro.actualizar(esperada)).thenReturn(esperada);

        assertThat(actualizar.ejecutar(4L, CAMBIOS)).isEqualTo(esperada);
    }

    @Test
    void laFechaFuturaTambienSeRechazaAlEditar() {
        DatosMascota futura = new DatosMascota("Max", Especie.PERRO, "Beagle", LocalDate.of(2026, 11, 14),
                BigDecimal.TEN);

        assertThatThrownBy(() -> actualizar.ejecutar(4L, futura))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de nacimiento no puede ser futura.");

        verifyNoInteractions(consulta, registro);
    }
}
