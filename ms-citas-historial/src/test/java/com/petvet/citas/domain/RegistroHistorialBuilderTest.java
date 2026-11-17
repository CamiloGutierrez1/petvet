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
                .vacunasAplicadas(List.of("Rabia"))
                .tratamientos(List.of("Tratamiento ficticio"))
                .examenes(List.of("Examen ficticio"))
                .cirugias(List.of("Cirugía ficticia"))
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
        assertThat(registro.vacunasAplicadas()).containsExactly("Rabia");
        assertThat(registro.tratamientos()).containsExactly("Tratamiento ficticio");
        assertThat(registro.examenes()).containsExactly("Examen ficticio");
        assertThat(registro.cirugias()).containsExactly("Cirugía ficticia");
        assertThat(registro.observaciones()).isEqualTo("Datos académicos");
        assertThat(registro.proximoControl()).isEqualTo(LocalDate.of(2026, 11, 23));
    }

    @Test
    void permiteUnaConsultaConSoloDiagnostico() {
        RegistroHistorial registro = nuevoBuilder().diagnostico("Revisión de demo").build();

        assertThat(registro.vacunasAplicadas()).isEmpty();
        assertThat(registro.tratamientos()).isEmpty();
        assertThat(registro.examenes()).isEmpty();
        assertThat(registro.cirugias()).isEmpty();
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
        List<String> vacunas = new ArrayList<>(List.of("Rabia"));
        List<String> tratamientos = new ArrayList<>(List.of("Tratamiento inicial"));
        List<String> examenes = new ArrayList<>(List.of("Examen inicial"));
        List<String> cirugias = new ArrayList<>(List.of("Cirugía inicial"));
        RegistroHistorial registro = nuevoBuilder()
                .diagnostico("Revisión de demo")
                .vacunasAplicadas(vacunas)
                .tratamientos(tratamientos)
                .examenes(examenes)
                .cirugias(cirugias)
                .build();

        vacunas.clear();
        tratamientos.clear();
        examenes.clear();
        cirugias.clear();

        assertThat(registro.vacunasAplicadas()).containsExactly("Rabia");
        assertThat(registro.tratamientos()).containsExactly("Tratamiento inicial");
        assertThat(registro.examenes()).containsExactly("Examen inicial");
        assertThat(registro.cirugias()).containsExactly("Cirugía inicial");
        assertThatThrownBy(() -> registro.vacunasAplicadas().add("Otra"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> registro.tratamientos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> registro.examenes().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> registro.cirugias().add("Otra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rechazaUnaCirugiaSinTexto(String vacia) {
        assertThatThrownBy(() -> nuevoBuilder().diagnostico("Revisión de demo")
                .cirugias(List.of("Esterilización", vacia)).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Los elementos del historial deben contener texto");
    }

    @Test
    void recortaLosEspaciosDeLasCirugias() {
        RegistroHistorial registro = nuevoBuilder().diagnostico("Revisión de demo")
                .cirugias(List.of("  Esterilización ")).build();

        assertThat(registro.cirugias()).containsExactly("Esterilización");
    }

    private RegistroHistorialBuilder nuevoBuilder() {
        return new RegistroHistorialBuilder(7L, 3L, 2L, FECHA);
    }
}
