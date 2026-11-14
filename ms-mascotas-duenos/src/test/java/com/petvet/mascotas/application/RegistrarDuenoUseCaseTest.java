package com.petvet.mascotas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.mascotas.domain.ConflictoException;
import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.RegistroDuenos;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarDuenoUseCaseTest {

    private static final Dueno LAURA = new Dueno(1L, "Laura Méndez", null, "laura.mendez@correo.com",
            "3104567812", null);

    @Mock private ConsultaDuenos consulta;
    @Mock private RegistroDuenos registro;
    private RegistrarDuenoUseCase registrar;

    @BeforeEach
    void prepararCasoDeUso() {
        registrar = new RegistrarDuenoUseCase(consulta, registro);
    }

    @Test
    void correoDuplicadoRespondeConflictoSinGuardarAunqueCambienMayusculasYEspacios() {
        when(consulta.buscarPorCorreo("laura.mendez@correo.com")).thenReturn(Optional.of(LAURA));

        assertThatThrownBy(() -> registrar.ejecutar(
                new DatosDueno("Laura M.", null, "  Laura.Mendez@Correo.COM ", null, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un dueño registrado con ese correo.");

        verify(registro, never()).guardar(any());
    }

    @Test
    void guardaElCorreoSinEspaciosEnMinusculasYLosOpcionalesVaciosComoNulos() {
        when(consulta.buscarPorCorreo("ana.prueba@correo.com")).thenReturn(Optional.empty());
        Dueno guardado = new Dueno(3L, "Ana Prueba", null, "ana.prueba@correo.com", null, "Calle 1");
        when(registro.guardar(new Dueno(null, "Ana Prueba", null, "ana.prueba@correo.com", null, "Calle 1")))
                .thenReturn(guardado);

        Dueno dueno = registrar.ejecutar(new DatosDueno(" Ana Prueba ", "  ", " Ana.Prueba@Correo.com", null, " Calle 1 "));

        assertThat(dueno).isEqualTo(guardado);
    }

    @Test
    void rechazaUnCorreoSinPuntoEnElDominioComoLoHaceCitas() {
        assertThatThrownBy(() -> registrar.ejecutar(new DatosDueno("Ana", null, "ana@clinica", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("El correo del dueño no es válido.");

        verifyNoInteractions(consulta, registro);
    }

    @Test
    void exigeLosDatosYElCorreo() {
        assertThatThrownBy(() -> registrar.ejecutar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Los datos del dueño son obligatorios.");
        assertThatThrownBy(() -> registrar.ejecutar(new DatosDueno("Ana", null, "   ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El correo del dueño es obligatorio.");

        verifyNoInteractions(consulta, registro);
    }
}
