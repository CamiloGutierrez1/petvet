package com.petvet.mascotas.presentation;

import com.petvet.mascotas.application.ConsultarFotoMascotaUseCase;
import com.petvet.mascotas.application.SubirFotoMascotaUseCase;
import com.petvet.mascotas.domain.FotoGuardada;
import com.petvet.mascotas.domain.FotoMascota;
import com.petvet.mascotas.presentation.dto.MascotaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Fotos de mascotas", description = "Subida y descarga de la foto de una mascota")
public class FotosMascotaController {
    private final SubirFotoMascotaUseCase subir;
    private final ConsultarFotoMascotaUseCase consultar;

    public FotosMascotaController(SubirFotoMascotaUseCase subir, ConsultarFotoMascotaUseCase consultar) {
        this.subir = subir;
        this.consultar = consultar;
    }

    @PostMapping(path = "/mascotas/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir la foto de una mascota",
            description = "multipart/form-data con el campo foto: JPG, PNG o WebP de hasta 5 MB. "
                    + "Devuelve la mascota con su fotoUrl absoluta.")
    public MascotaResponse subir(@PathVariable @Positive long id, @RequestPart("foto") MultipartFile foto)
            throws IOException {
        return MascotaResponse.desde(subir.ejecutar(id, foto.getBytes(), foto.getContentType()));
    }

    @GetMapping("/mascotas/{id}/foto")
    @Operation(summary = "Descargar la foto subida",
            description = "Devuelve los bytes con su Content-Type, o 404 si la mascota no tiene una foto subida. "
                    + "La ETag es la versión de la foto: con un If-None-Match igual responde 304 sin los bytes.")
    public ResponseEntity<byte[]> foto(@PathVariable @Positive long id) {
        FotoGuardada guardada = consultar.ejecutar(id);
        FotoMascota foto = guardada.foto();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                // El tipo ya se validó con los bytes; nosniff evita que el navegador lo reinterprete.
                .header("X-Content-Type-Options", "nosniff")
                // El navegador guarda la foto y la revalida antes de usarla: si su If-None-Match trae esta ETag,
                // Spring responde 304 sin los bytes. El parámetro v de la fotoUrl sigue cambiando en cada subida.
                .cacheControl(CacheControl.noCache())
                .eTag(String.valueOf(guardada.version()))
                .body(foto.datos());
    }
}
