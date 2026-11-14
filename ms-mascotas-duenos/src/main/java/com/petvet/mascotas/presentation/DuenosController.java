package com.petvet.mascotas.presentation;

import com.petvet.mascotas.application.ConsultarDuenosUseCase;
import com.petvet.mascotas.application.RegistrarDuenoUseCase;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.presentation.dto.DuenoResponse;
import com.petvet.mascotas.presentation.dto.NuevoDuenoRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Dueños", description = "Registro y consulta de dueños")
public class DuenosController {
    private final RegistrarDuenoUseCase registrar;
    private final ConsultarDuenosUseCase consultar;

    public DuenosController(RegistrarDuenoUseCase registrar, ConsultarDuenosUseCase consultar) {
        this.registrar = registrar;
        this.consultar = consultar;
    }

    @PostMapping("/duenos")
    @Operation(summary = "Registrar un dueño",
            description = "Guarda el correo sin espacios y en minúsculas. Responde 409 si ya existe un dueño con ese correo.")
    public ResponseEntity<DuenoResponse> registrar(@Valid @RequestBody NuevoDuenoRequest datos) {
        Dueno dueno = registrar.ejecutar(datos.aDatos());
        return ResponseEntity.created(URI.create("/api/duenos/" + dueno.id())).body(DuenoResponse.desde(dueno));
    }

    @GetMapping("/duenos")
    @Operation(summary = "Listar los dueños", description = "Arreglo plano con todos los dueños, ordenados por id.")
    public List<DuenoResponse> listar() {
        return consultar.listar().stream().map(DuenoResponse::desde).toList();
    }

    @GetMapping("/duenos/{id}")
    @Operation(summary = "Consultar un dueño", description = "Devuelve el dueño o 404 si no existe.")
    public DuenoResponse dueno(@PathVariable @Positive long id) {
        return DuenoResponse.desde(consultar.buscar(id));
    }
}
