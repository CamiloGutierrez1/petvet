package com.petvet.mascotas.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MascotaResponseTest {

    @Test
    void elPesoDeNumericSaleSinCerosSobrantesNiNotacionCientifica() {
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("31.50"))).hasToString("31.5");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("0.04"))).hasToString("0.04");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("4.20"))).hasToString("4.2");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("100.00"))).hasToString("100");
    }
}
