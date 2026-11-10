package com.petvet.citas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RegistroHistorialBuilderTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 9, 9, 30);

    @Test
    void construyeUnRegistroConCamposOpcionales() {
        RegistroHistorial registro = nuevoBuilder()
                .diagnostico("Consulta de demostración")
                .tipoConsulta(TipoConsulta.ESPECIALISTA)
                .vacunasAplicadas(List.of("Vacuna demo adulto"))
                .tratamientos(List.of("Tratamiento ficticio"))
                .examenes(List.of("Examen ficticio"))
                .observaciones("Datos académicos")
                .proximoControl(LocalDate.of(2026, 11, 23))
                .build();

        assertThat(registro.id()).isNull();
        assertThat(registro.citaId()).isEqualTo(7L);
        assertThat(registro.mascotaId()).isEqualTo(3L);
        assertThat(registro.veterinarioId()).isEqualTo(2L);
        assertThat(registro.fecha()).isEqualTo(FECHA);
        assertThat(registro.tipoConsulta()).isEqualTo(TipoConsulta.ESPECIALISTA);
        assertThat(registro.diagnostico()).isEqualTo("Consulta de demostración");
        assertThat(registro.vacunasAplicadas()).containsExactly("Vacuna demo adulto");
        assertThat(registro.tratamientos()).containsExactly("Tratamiento ficticio");
        assertThat(registro.examenes()).containsExactly("Examen ficticio");
        assertThat(registro.observaciones()).isEqualTo("Datos académicos");
        assertThat(registro.proximoControl()).isEqualTo(LocalDate.of(2026, 11, 23));
    }

    @Test
    void permiteUnaConsultaConSoloDiagnostico() {
        RegistroHistorial registro = nuevoBuilder().diagnostico("Revisión de demo").build();

        assertThat(registro.vacunasAplicadas()).isEmpty();
        assertThat(registro.tratamientos()).isEmpty();
        assertThat(registro.examenes()).isEmpty();
        assertThat(registro.proximoControl()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void exigeUnDiagnosticoConContenido(String diagnostico) {
        assertThatThrownBy(() -> nuevoBuilder().diagnostico(diagnostico).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elRegistroConservaUnaCopiaInmutableDeLasListas() {
        List<String> vacunas = new ArrayList<>(List.of("Vacuna demo adulto"));
        List<String> tratamientos = new ArrayList<>(List.of("Tratamiento inicial"));
        List<String> examenes = new ArrayList<>(List.of("Examen inicial"));
        RegistroHistorial registro = nuevoBuilder()
                .diagnostico("Revisión de demo")
                .vacunasAplicadas(vacunas)
                .tratamientos(tratamientos)
                .examenes(examenes)
                .build();

        vacunas.clear();
        tratamientos.clear();
        examenes.clear();

        assertThat(registro.vacunasAplicadas()).containsExactly("Vacuna demo adulto");
        assertThat(registro.tratamientos()).containsExactly("Tratamiento inicial");
        assertThat(registro.examenes()).containsExactly("Examen inicial");
        assertThatThrownBy(() -> registro.vacunasAplicadas().add("Otra"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> registro.tratamientos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> registro.examenes().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private RegistroHistorialBuilder nuevoBuilder() {
        return new RegistroHistorialBuilder(7L, 3L, 2L, FECHA);
    }
}
