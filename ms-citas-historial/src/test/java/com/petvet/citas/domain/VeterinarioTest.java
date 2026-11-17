package com.petvet.citas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class VeterinarioTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DIECIOCHO = LocalTime.of(18, 0);

    @Test
    void conservaSuHorarioPropioAunqueHayaUnoPorDefecto() {
        Veterinario rios = new Veterinario(3L, "Dra. Valentina Ríos", "Cirugía",
                LocalTime.of(7, 0), LocalTime.of(13, 0));

        assertThat(rios.tieneHorario()).isTrue();
        assertThat(rios.conHorarioPorDefecto(OCHO, DIECIOCHO)).isSameAs(rios);
    }

    @Test
    void sinHorarioPropioTomaElHorarioPorDefecto() {
        Veterinario sinHorario = new Veterinario(1L, "Dra. Paula Castaño", "Medicina general");

        assertThat(sinHorario.tieneHorario()).isFalse();
        assertThat(sinHorario.conHorarioPorDefecto(OCHO, DIECIOCHO)).isEqualTo(
                new Veterinario(1L, "Dra. Paula Castaño", "Medicina general", OCHO, DIECIOCHO));
    }

    @Test
    void rechazaUnHorarioIncompletoOQueNoAvanza() {
        assertThatThrownBy(() -> new Veterinario(1L, "Vet demo", "General", OCHO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Veterinario(1L, "Vet demo", "General", null, DIECIOCHO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Veterinario(1L, "Vet demo", "General", DIECIOCHO, OCHO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hora de inicio del veterinario debe ser anterior a la hora de fin");
        assertThatThrownBy(() -> new Veterinario(1L, "Vet demo", "General", OCHO, OCHO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
