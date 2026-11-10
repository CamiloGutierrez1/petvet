package com.petvet.citas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConsultaTest {

    @ParameterizedTest
    @MethodSource("consultas")
    void lasImplementacionesCumplenElMismoContrato(Consulta consulta, TipoConsulta tipo) {
        RegistroHistorialBuilder builder = nuevoBuilder().diagnostico("Consulta de demo");

        RegistroHistorial registro = consulta.registrar(builder);

        assertThat(registro.tipoConsulta()).isEqualTo(tipo);
        assertThat(registro.diagnostico()).isEqualTo("Consulta de demo");
        assertThat(registro.citaId()).isEqualTo(7L);
        assertThat(registro.examenes()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("consultas")
    void ambasImplementacionesExigenDiagnostico(Consulta consulta, TipoConsulta tipo) {
        assertThatThrownBy(() -> consulta.registrar(nuevoBuilder()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> consultas() {
        return Stream.of(
                Arguments.of(new ConsultaGeneral(), TipoConsulta.GENERAL),
                Arguments.of(new ConsultaEspecialista(), TipoConsulta.ESPECIALISTA));
    }

    private RegistroHistorialBuilder nuevoBuilder() {
        return new RegistroHistorialBuilder(7L, 3L, 2L, LocalDateTime.of(2026, 11, 9, 10, 0));
    }
}
