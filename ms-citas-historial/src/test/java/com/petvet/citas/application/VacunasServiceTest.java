package com.petvet.citas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.EsquemaAdulto;
import com.petvet.citas.domain.EsquemaCachorro;
import com.petvet.citas.domain.EsquemaGeriatrico;
import com.petvet.citas.domain.HistorialRepository;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import com.petvet.citas.domain.PublicadorEventos;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import com.petvet.citas.domain.SelectorEsquemaVacunacion;
import com.petvet.citas.domain.VacunaPendiente;
import com.petvet.citas.domain.eventos.VacunaProximaEvento;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacunasServiceTest {

    private static final long MASCOTA_ID = 3L;
    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-10T02:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    @Mock
    private HistorialRepository historiales;
    @Mock
    private MascotaClient mascotas;
    @Mock
    private PublicadorEventos eventos;

    private final TransaccionesDePrueba transacciones = new TransaccionesDePrueba();
    private VacunasService servicio;

    @BeforeEach
    void prepararServicio() {
        SelectorEsquemaVacunacion selector = new SelectorEsquemaVacunacion(List.of(
                new EsquemaCachorro(), new EsquemaAdulto(), new EsquemaGeriatrico()));
        servicio = new VacunasService(historiales, mascotas, selector, eventos, RELOJ, transacciones, 7);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void publicaEnAmbosLimitesDeLaVentanaUsandoLaFechaDeBogota(int diasHastaVencimiento) {
        LocalDate vencimiento = HOY.plusDays(diasHastaVencimiento);
        prepararVacunaPendiente(vencimiento);

        servicio.publicarRecordatorios(MASCOTA_ID);

        ArgumentCaptor<VacunaProximaEvento> captor = ArgumentCaptor.forClass(VacunaProximaEvento.class);
        verify(eventos).vacunaProxima(captor.capture());
        VacunaProximaEvento evento = captor.getValue();
        assertThat(evento.mascotaId()).isEqualTo(MASCOTA_ID);
        assertThat(evento.mascotaNombre()).isEqualTo("Mascota de demostración");
        assertThat(evento.duenoEmail()).isEqualTo("dueno@example.test");
        assertThat(evento.vacuna()).isEqualTo("Vacuna demo cachorro");
        assertThat(evento.fechaLimite()).isEqualTo(vencimiento);
        verifyNoMoreInteractions(eventos);
        assertThat(transacciones.abiertas()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 8})
    void omiteVacunasVencidasOLoBastanteLejanas(int diasHastaVencimiento) {
        prepararVacunaPendiente(HOY.plusDays(diasHastaVencimiento));

        servicio.publicarRecordatorios(MASCOTA_ID);

        verifyNoInteractions(eventos);
        assertThat(transacciones.abiertas()).as("sin recordatorios no se abre transacción").isZero();
    }

    @Test
    void propagaMascotaNoEncontradaSinRegistrarEventosNiConsultarSuHistorial() {
        RecursoNoEncontradoException error = new RecursoNoEncontradoException("Mascota inexistente");
        when(mascotas.buscar(MASCOTA_ID)).thenThrow(error);

        assertThatThrownBy(() -> servicio.publicarRecordatorios(MASCOTA_ID)).isSameAs(error);

        verifyNoInteractions(historiales, eventos);
    }

    @Test
    void consultarVacunasPendientesNoPublicaRecordatorios() {
        LocalDate vencimiento = HOY.plusDays(2);
        prepararVacunaPendiente(vencimiento);

        List<VacunaPendiente> resultado = servicio.proximas(MASCOTA_ID);

        assertThat(resultado).containsExactly(new VacunaPendiente("Vacuna demo cachorro", vencimiento));
        verifyNoInteractions(eventos);
    }

    private void prepararVacunaPendiente(LocalDate vencimiento) {
        Mascota mascota = new Mascota(MASCOTA_ID, "Mascota de demostración", Especie.PERRO,
                vencimiento.minusMonths(2), "Dueño de demostración", "dueno@example.test");
        when(mascotas.buscar(MASCOTA_ID)).thenReturn(mascota);
        when(historiales.porMascota(MASCOTA_ID)).thenReturn(List.of());
    }
}
