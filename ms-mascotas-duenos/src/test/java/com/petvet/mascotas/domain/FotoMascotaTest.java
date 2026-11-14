package com.petvet.mascotas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FotoMascotaTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @Test
    void reconoceJpegPngYWebpPorSusPrimerosBytes() {
        assertThat(FotoMascota.desde(JPEG, "image/jpeg").tipoContenido()).isEqualTo("image/jpeg");
        assertThat(FotoMascota.desde(PNG, "image/png").tipoContenido()).isEqualTo("image/png");
        assertThat(FotoMascota.desde(WEBP, "image/webp").tipoContenido()).isEqualTo("image/webp");
    }

    @Test
    void guardaElTipoQueIndicanLosBytesYAceptaParametrosEnElDeclarado() {
        assertThat(FotoMascota.desde(JPEG, "IMAGE/PNG; charset=binary").tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    void rechazaUnTextoAunqueSeDeclareComoImagen() {
        byte[] texto = "no soy una imagen".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> FotoMascota.desde(texto, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La foto debe ser una imagen JPG, PNG o WebP.");
    }

    @Test
    void rechazaUnTipoDeclaradoDistintoDeJpgPngOWebp() {
        assertThatThrownBy(() -> FotoMascota.desde(PNG, "image/gif"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La foto debe ser una imagen JPG, PNG o WebP.");
        assertThatThrownBy(() -> FotoMascota.desde(PNG, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnaFotoVaciaOMayorDe5MB() {
        byte[] grande = Arrays.copyOf(PNG, FotoMascota.TAMANO_MAXIMO + 1);

        assertThatThrownBy(() -> FotoMascota.desde(new byte[0], "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La foto está vacía.");
        assertThatThrownBy(() -> FotoMascota.desde(grande, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La foto pesa más de 5 MB.");
        assertThat(FotoMascota.desde(Arrays.copyOf(PNG, FotoMascota.TAMANO_MAXIMO), "image/png").datos())
                .hasSize(5 * 1024 * 1024);
    }
}
