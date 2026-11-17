package com.petvet.mascotas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import com.petvet.mascotas.domain.RegistroMascotas;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarMascotaUseCaseTest {

    // 11:30 p. m. del 13 de noviembre en Bogotá. En UTC ya es 14 de noviembre.
    private static final Instant CASI_MEDIANOCHE = Instant.parse("2026-11-14T04:30:00Z");
    private static final LocalDate HOY_EN_BOGOTA = LocalDate.of(2026, 11, 13);
    private static final Dueno LAURA = new Dueno(1L, "Laura Méndez", null, "laura.mendez@correo.com", null, null);

    @Mock private RegistroMascotas registro;
    @Mock private ConsultaDuenos duenos;
    private RegistrarMascotaUseCase registrar;

    @BeforeEach
    void prepararCasoDeUso() {
        registrar = new RegistrarMascotaUseCase(registro, duenos,
                Clock.fixed(CASI_MEDIANOCHE, ZoneId.of("America/Bogota")));
    }

    @Test
    void fechaDeNacimientoFuturaEnBogotaSeRechazaAunqueEnUtcYaSeaEseDia() {
        LocalDate manana = HOY_EN_BOGOTA.plusDays(1);
        assertThat(LocalDate.ofInstant(CASI_MEDIANOCHE, ZoneOffset.UTC)).isEqualTo(manana);

        assertThatThrownBy(() -> registrar.ejecutar(1L, datos(manana, new BigDecimal("3.5"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de nacimiento no puede ser futura.");

        verifyNoInteractions(registro, duenos);
    }

    @Test
    void unaMascotaQueNacioHoyEnBogotaSeRegistraANombreDelDuenoDeLaRuta() {
        when(duenos.buscarPorId(1L)).thenReturn(Optional.of(LAURA));
        Mascota nueva = new Mascota(null, "Luna", Especie.GATO, "Criollo", HOY_EN_BOGOTA, new BigDecimal("3.5"), null, 1L);
        Mascota guardada = new Mascota(4L, "Luna", Especie.GATO, "Criollo", HOY_EN_BOGOTA, new BigDecimal("3.5"), null, 1L);
        when(registro.guardar(nueva)).thenReturn(guardada);

        Mascota mascota = registrar.ejecutar(1L, new DatosMascota(" Luna ", Especie.GATO, " Criollo ",
                HOY_EN_BOGOTA, new BigDecimal("3.5")));

        assertThat(mascota).isEqualTo(guardada);
    }

    @Test
    void duenoInexistenteRespondeNoEncontradoSinGuardar() {
        when(duenos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrar.ejecutar(99L, datos(LocalDate.of(2024, 2, 11), new BigDecimal("0.04"))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el dueño 99");

        verify(registro, never()).guardar(any());
    }

    @Test
    void pesoCeroONegativoYFechaAnteriorA1900SeRechazan() {
        assertThatThrownBy(() -> registrar.ejecutar(1L, datos(HOY_EN_BOGOTA, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El peso debe ser mayor que 0.");
        assertThatThrownBy(() -> registrar.ejecutar(1L, datos(HOY_EN_BOGOTA, new BigDecimal("-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El peso debe ser mayor que 0.");
        assertThatThrownBy(() -> registrar.ejecutar(1L, datos(LocalDate.of(1899, 12, 31), BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha de nacimiento no puede ser anterior a 1900.");

        verifyNoInteractions(registro, duenos);
    }

    private static DatosMascota datos(LocalDate fechaNacimiento, BigDecimal peso) {
        return new DatosMascota("Luna", Especie.GATO, "Criollo", fechaNacimiento, peso);
    }
}
