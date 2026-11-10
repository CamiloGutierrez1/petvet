package com.petvet.citas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VacunacionStrategyTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    @ParameterizedTest
    @EnumSource(value = Especie.class, names = {"PERRO", "GATO"})
    void losRangosDeEdadNoDejanHuecosNiSeSolapan(Especie especie) {
        VacunacionStrategy cachorro = new EsquemaCachorro();
        VacunacionStrategy adulto = new EsquemaAdulto();
        VacunacionStrategy geriatrico = new EsquemaGeriatrico();

        assertThat(cachorro.aplica(especie, -1)).isFalse();
        assertThat(cachorro.aplica(especie, 0)).isTrue();
        assertThat(cachorro.aplica(especie, 11)).isTrue();
        assertThat(cachorro.aplica(especie, 12)).isFalse();
        assertThat(adulto.aplica(especie, 11)).isFalse();
        assertThat(adulto.aplica(especie, 12)).isTrue();
        assertThat(adulto.aplica(especie, 83)).isTrue();
        assertThat(adulto.aplica(especie, 84)).isFalse();
        assertThat(geriatrico.aplica(especie, 83)).isFalse();
        assertThat(geriatrico.aplica(especie, 84)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Especie.class, names = {"AVE", "OTRO"})
    void unaEspecieSinEsquemaDevuelveUnaListaVacia(Especie especie) {
        assertThat(selector().proximasVacunas(mascota(especie, HOY.minusYears(2)), List.of(), HOY))
                .isEmpty();
        assertThat(new EsquemaCachorro().aplica(especie, 2)).isFalse();
        assertThat(new EsquemaAdulto().aplica(especie, 24)).isFalse();
        assertThat(new EsquemaGeriatrico().aplica(especie, 100)).isFalse();
    }

    @Test
    void elSelectorUsaMesesCumplidosAlCambiarDeEsquema() {
        Mascota mascota = mascota(Especie.PERRO, LocalDate.of(2025, 11, 10));

        assertThat(selector().proximasVacunas(mascota, List.of(), HOY))
                .extracting(VacunaPendiente::vacuna)
                .containsExactly("Vacuna demo cachorro");
        assertThat(selector().proximasVacunas(mascota, List.of(), HOY.plusDays(1)))
                .extracting(VacunaPendiente::vacuna)
                .containsExactly("Vacuna demo adulto");
    }

    @Test
    void rechazaUnaFechaDeNacimientoFutura() {
        assertThatThrownBy(() -> selector().proximasVacunas(
                mascota(Especie.PERRO, HOY.plusDays(1)), List.of(), HOY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elEsquemaCachorroProponeUnaDosisYLaRetiraAlEstarAplicada() {
        Mascota mascota = mascota(Especie.GATO, LocalDate.of(2026, 9, 15));
        EsquemaCachorro esquema = new EsquemaCachorro();

        assertThat(esquema.proximasVacunas(mascota, List.of(), HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo cachorro", LocalDate.of(2026, 11, 15)));
        assertThat(esquema.proximasVacunas(mascota,
                List.of(registro("Vacuna demo cachorro", LocalDate.of(2026, 10, 30))), HOY))
                .isEmpty();
    }

    @Test
    void elEsquemaAdultoCalculaElRefuerzoDesdeLaUltimaAplicacion() {
        Mascota mascota = mascota(Especie.PERRO, LocalDate.of(2023, 5, 4));
        List<RegistroHistorial> historial = List.of(
                registro("Vacuna demo adulto", LocalDate.of(2024, 5, 20)),
                registro("Otra vacuna de demo", LocalDate.of(2026, 10, 30)),
                registro("Vacuna demo adulto", LocalDate.of(2025, 11, 12)));

        assertThat(new EsquemaAdulto().proximasVacunas(mascota, historial, HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo adulto", LocalDate.of(2026, 11, 12)));
    }

    @Test
    void unaDosisInicialVencidaSiguePendiente() {
        Mascota mascota = mascota(Especie.GATO, LocalDate.of(2024, 1, 31));

        assertThat(new EsquemaAdulto().proximasVacunas(mascota, List.of(), HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo adulto", LocalDate.of(2025, 1, 31)));
    }

    @Test
    void elRefuerzoAnualRespetaElCalendarioBisiesto() {
        Mascota mascota = mascota(Especie.PERRO, LocalDate.of(2022, 1, 1));

        assertThat(new EsquemaAdulto().proximasVacunas(mascota,
                List.of(registro("Vacuna demo adulto", LocalDate.of(2024, 2, 29))), HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo adulto", LocalDate.of(2025, 2, 28)));
    }

    @Test
    void elEsquemaGeriatricoUsaElUltimoRegistroParaSuRefuerzoSemestral() {
        Mascota mascota = mascota(Especie.GATO, LocalDate.of(2017, 3, 9));

        assertThat(new EsquemaGeriatrico().proximasVacunas(mascota, List.of(), HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo geriátrico", LocalDate.of(2024, 3, 9)));
        assertThat(new EsquemaGeriatrico().proximasVacunas(mascota,
                List.of(registro("Vacuna demo geriátrico", LocalDate.of(2026, 8, 31))), HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo geriátrico", LocalDate.of(2027, 2, 28)));
    }

    @Test
    void admiteUnaNuevaEspecieMedianteOtraImplementacion() {
        VacunacionStrategy esquemaAve = new VacunacionStrategy() {
            @Override
            public boolean aplica(Especie especie, int edadMeses) {
                return especie == Especie.AVE;
            }

            @Override
            public List<VacunaPendiente> proximasVacunas(
                    Mascota mascota, List<RegistroHistorial> historial, LocalDate hoy) {
                return List.of(new VacunaPendiente("Vacuna demo ave", hoy.plusDays(7)));
            }
        };
        List<VacunacionStrategy> estrategias = new ArrayList<>(List.of(
                new EsquemaCachorro(), new EsquemaAdulto(), new EsquemaGeriatrico(), esquemaAve));
        SelectorEsquemaVacunacion selector = new SelectorEsquemaVacunacion(estrategias);
        estrategias.clear();

        assertThat(selector.proximasVacunas(mascota(Especie.AVE, HOY.minusYears(1)), List.of(), HOY))
                .containsExactly(new VacunaPendiente("Vacuna demo ave", HOY.plusDays(7)));
    }

    private SelectorEsquemaVacunacion selector() {
        return new SelectorEsquemaVacunacion(List.of(
                new EsquemaCachorro(), new EsquemaAdulto(), new EsquemaGeriatrico()));
    }

    private Mascota mascota(Especie especie, LocalDate nacimiento) {
        return new Mascota(1L, "Mascota demo", especie, nacimiento, "Dueño demo", "demo@example.test");
    }

    private RegistroHistorial registro(String vacuna, LocalDate fecha) {
        return new RegistroHistorialBuilder(1L, 1L, 1L, fecha.atTime(9, 0))
                .diagnostico("Consulta de demostración")
                .vacunasAplicadas(List.of(vacuna))
                .build();
    }
}
