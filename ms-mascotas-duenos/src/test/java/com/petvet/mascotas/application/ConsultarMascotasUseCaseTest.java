package com.petvet.mascotas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarMascotasUseCaseTest {

    private static final Mascota TOBBY = new Mascota(1L, "Tobby", Especie.PERRO, "Golden Retriever",
            LocalDate.of(2021, 4, 18), new BigDecimal("31.50"), "/mascotas/tobby.jpg", 1L);
    private static final Dueno LAURA = new Dueno(1L, "Laura Méndez", null, "laura.mendez@correo.com", null, null);

    @Mock private ConsultaMascotas mascotas;
    @Mock private ConsultaDuenos duenos;
    private ConsultarMascotasUseCase consultar;

    @BeforeEach
    void prepararCasoDeUso() {
        consultar = new ConsultarMascotasUseCase(mascotas, duenos);
    }

    @Test
    void mascotaInexistenteRespondeNoEncontradoConElTextoQueEsperaCitas() {
        when(mascotas.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultar.ficha(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe la mascota 999");

        verifyNoInteractions(duenos);
    }

    @Test
    void laFichaUneLaMascotaConSuDueno() {
        when(mascotas.buscarPorId(1L)).thenReturn(Optional.of(TOBBY));
        when(duenos.buscarPorId(1L)).thenReturn(Optional.of(LAURA));

        assertThat(consultar.ficha(1L)).isEqualTo(new FichaMascota(TOBBY, LAURA));
    }

    @Test
    void lasMascotasDeUnDuenoInexistenteRespondenNoEncontrado() {
        when(duenos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultar.deDueno(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el dueño 99");

        verifyNoInteractions(mascotas);
    }

    @Test
    void listaLasMascotasDeUnDuenoExistente() {
        when(duenos.buscarPorId(1L)).thenReturn(Optional.of(LAURA));
        when(mascotas.buscarPorDueno(1L)).thenReturn(List.of(TOBBY));

        assertThat(consultar.deDueno(1L)).containsExactly(TOBBY);
    }
}
