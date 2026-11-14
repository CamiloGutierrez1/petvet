package com.petvet.mascotas.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MascotaResponseTest {

    @Test
    void elPesoDeNumericSaleSinCerosSobrantesNiNotacionCientifica() {
        // Así devuelve NUMERIC(7,3) los datos de ejemplo y los pesos con gramos.
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("31.500"))).hasToString("31.5");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("0.040"))).hasToString("0.04");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("4.200"))).hasToString("4.2");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("0.035"))).hasToString("0.035");
        assertThat(MascotaResponse.sinCerosSobrantes(new BigDecimal("100.000"))).hasToString("100");
    }
}
