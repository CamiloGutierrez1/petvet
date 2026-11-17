package com.petvet.mascotas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.mascotas.domain.AlmacenFotosMascota;
import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.FotoMascota;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubirFotoMascotaUseCaseTest {

    private static final long AHORA = 1_763_000_000_000L;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final Mascota MAX = new Mascota(4L, "Max", Especie.PERRO, "Beagle", LocalDate.of(2025, 6, 1),
            new BigDecimal("12.00"), null, 2L);

    @Mock private ConsultaMascotas mascotas;
    @Mock private AlmacenFotosMascota fotos;
    private SubirFotoMascotaUseCase subir;

    @BeforeEach
    void prepararCasoDeUso() {
        subir = new SubirFotoMascotaUseCase(mascotas, fotos,
                Clock.fixed(Instant.ofEpochMilli(AHORA), ZoneId.of("America/Bogota")));
    }

    @Test
    void guardaLaFotoConLaVersionDelRelojYDevuelveLaMascotaConSuNuevaUrl() {
        Mascota conFoto = MAX.conFoto("http://localhost:8081/api/mascotas/4/foto?v=" + AHORA);
        when(mascotas.buscarPorId(4L)).thenReturn(Optional.of(MAX), Optional.of(conFoto));

        Mascota resultado = subir.ejecutar(4L, PNG, "image/png");

        ArgumentCaptor<FotoMascota> foto = ArgumentCaptor.forClass(FotoMascota.class);
        verify(fotos).actualizarFoto(eq(4L), foto.capture(), eq(AHORA));
        assertThat(foto.getValue().tipoContenido()).isEqualTo("image/png");
        assertThat(foto.getValue().datos()).isEqualTo(PNG);
        assertThat(resultado).isEqualTo(conFoto);
    }

    @Test
    void mascotaInexistenteNoGuardaLaFoto() {
        when(mascotas.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subir.ejecutar(999L, PNG, "image/png"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe la mascota 999");

        verifyNoInteractions(fotos);
    }

    @Test
    void unArchivoQueNoEsImagenSeRechazaAntesDeConsultarOGuardar() {
        assertThatThrownBy(() -> subir.ejecutar(4L, "hola".getBytes(StandardCharsets.UTF_8), "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La foto debe ser una imagen JPG, PNG o WebP.");

        verifyNoInteractions(mascotas, fotos);
    }
}
