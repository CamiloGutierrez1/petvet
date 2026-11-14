package com.petvet.mascotas.presentation;

import com.petvet.mascotas.application.ActualizarMascotaUseCase;
import com.petvet.mascotas.application.ConsultarMascotasUseCase;
import com.petvet.mascotas.application.RegistrarMascotaUseCase;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.presentation.dto.DatosMascotaRequest;
import com.petvet.mascotas.presentation.dto.FichaMascotaResponse;
import com.petvet.mascotas.presentation.dto.MascotaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Mascotas", description = "Registro, edición y ficha de las mascotas")
public class MascotasController {
    private final RegistrarMascotaUseCase registrar;
    private final ActualizarMascotaUseCase actualizar;
    private final ConsultarMascotasUseCase consultar;

    public MascotasController(RegistrarMascotaUseCase registrar, ActualizarMascotaUseCase actualizar,
            ConsultarMascotasUseCase consultar) {
        this.registrar = registrar;
        this.actualizar = actualizar;
        this.consultar = consultar;
    }

    @PostMapping("/duenos/{id}/mascotas")
    @Operation(summary = "Registrar una mascota",
            description = "Registra la mascota del dueño de la ruta. La fecha de nacimiento no puede ser futura en America/Bogota.")
    public ResponseEntity<MascotaResponse> registrar(@PathVariable @Positive long id,
            @Valid @RequestBody DatosMascotaRequest datos) {
        Mascota mascota = registrar.ejecutar(id, datos.aDatos());
        return ResponseEntity.created(URI.create("/api/mascotas/" + mascota.id())).body(MascotaResponse.desde(mascota));
    }

    @GetMapping("/duenos/{id}/mascotas")
    @Operation(summary = "Listar las mascotas de un dueño", description = "Arreglo ordenado por id, o 404 si el dueño no existe.")
    public List<MascotaResponse> mascotasDelDueno(@PathVariable @Positive long id) {
        return consultar.deDueno(id).stream().map(MascotaResponse::desde).toList();
    }

    @GetMapping("/mascotas/{id}")
    @Operation(summary = "Consultar la ficha de una mascota",
            description = "Ficha plana con el nombre y el correo del dueño. La consume Citas e Historial.")
    public FichaMascotaResponse ficha(@PathVariable @Positive long id) {
        return FichaMascotaResponse.desde(consultar.ficha(id));
    }

    @PutMapping("/mascotas/{id}")
    @Operation(summary = "Editar una mascota",
            description = "Cambia nombre, especie, raza, fecha de nacimiento y peso. Conserva la foto y el dueño.")
    public MascotaResponse editar(@PathVariable @Positive long id, @Valid @RequestBody DatosMascotaRequest datos) {
        return MascotaResponse.desde(actualizar.ejecutar(id, datos.aDatos()));
    }
}
