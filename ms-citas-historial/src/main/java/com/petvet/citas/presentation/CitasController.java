package com.petvet.citas.presentation;

import com.petvet.citas.application.*;
import com.petvet.citas.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "Citas e historial", description = "Gestión de citas y consultas de PetVet")
public class CitasController {
    // Mensajes de Bean Validation. ApiExceptionHandler los antepone con el nombre del campo.
    private static final String OBLIGATORIO = "es un campo obligatorio";
    private static final String SIN_TEXTO = "no puede estar vacío";
    private static final String POSITIVO = "debe ser mayor que cero";
    private static final String MAX_CARACTERES = "admite máximo {max} caracteres";
    private static final String MAX_ELEMENTOS = "admite máximo {max} elementos";

    private final AgendaService agenda;
    private final AgendarCitaUseCase agendar;
    private final RegistrarConsultaUseCase registrar;
    private final VacunasService vacunas;

    public CitasController(AgendaService agenda, AgendarCitaUseCase agendar,
            RegistrarConsultaUseCase registrar, VacunasService vacunas) {
        this.agenda = agenda;
        this.agendar = agendar;
        this.registrar = registrar;
        this.vacunas = vacunas;
    }

    @GetMapping("/veterinarios")
    public List<Veterinario> veterinarios() {
        return agenda.veterinarios();
    }

    @GetMapping("/veterinarios/{id}/disponibilidad")
    public AgendaService.Disponibilidad disponibilidad(@PathVariable @Positive(message = POSITIVO) long id,
            @RequestParam LocalDate fecha) {
        return agenda.disponibilidad(id, fecha);
    }

    @PostMapping("/citas")
    @Operation(summary = "Agendar una cita", description = "Reserva el turno y guarda cita.agendada para publicación durable.")
    public ResponseEntity<Cita> agendar(@Valid @RequestBody NuevaCita datos) {
        Cita cita = agendar.ejecutar(datos.mascotaId(), datos.veterinarioId(), datos.fechaHora(), datos.tipoServicio());
        return ResponseEntity.created(URI.create("/api/citas?mascotaId=" + cita.mascotaId())).body(cita);
    }

    @GetMapping("/citas/{id}")
    @Operation(summary = "Consultar una cita", description = "Devuelve la cita con su estado actual o 404 si no existe.")
    public Cita cita(@PathVariable @Positive(message = POSITIVO) long id) {
        return agenda.buscar(id);
    }

    @GetMapping("/citas")
    @Operation(summary = "Listar citas", description = "Filtra por mascotaId, por veterinarioId (todas sus citas, "
            + "de la más reciente a la más antigua) o por veterinarioId y fecha (agenda del día).")
    public List<Cita> citas(@RequestParam(required = false) @Positive(message = POSITIVO) Long mascotaId,
            @RequestParam(required = false) @Positive(message = POSITIVO) Long veterinarioId,
            @RequestParam(required = false) LocalDate fecha) {
        return agenda.listar(mascotaId, veterinarioId, fecha);
    }

    @PatchMapping("/citas/{id}/cancelar")
    public Cita cancelar(@PathVariable @Positive(message = POSITIVO) long id) {
        return agenda.cancelar(id);
    }

    @PostMapping("/citas/{id}/consulta")
    public ResponseEntity<RegistroHistorial> consulta(@PathVariable @Positive(message = POSITIVO) long id,
            @Valid @RequestBody NuevaConsulta datos) {
        RegistroHistorial registro = registrar.ejecutar(id, new DatosConsulta(datos.tipoConsulta(),
                datos.diagnostico(), datos.vacunasAplicadas(), datos.tratamientos(), datos.examenes(),
                datos.observaciones(), datos.proximoControl()));
        return ResponseEntity.created(URI.create("/api/mascotas/" + registro.mascotaId() + "/historial"))
                .body(registro);
    }

    @GetMapping("/mascotas/{id}/historial")
    public List<RegistroHistorial> historial(@PathVariable @Positive(message = POSITIVO) long id) {
        return vacunas.historial(id);
    }

    @GetMapping("/mascotas/{id}/proximas-vacunas")
    @Operation(summary = "Consultar vacunas de demostración", description = "Calendarios académicos ficticios por especie y edad.")
    public List<VacunaPendiente> proximasVacunas(@PathVariable @Positive(message = POSITIVO) long id) {
        return vacunas.proximas(id);
    }

    public record NuevaCita(
            @NotNull(message = OBLIGATORIO) @Positive(message = POSITIVO) Long mascotaId,
            @NotNull(message = OBLIGATORIO) @Positive(message = POSITIVO) Long veterinarioId,
            @NotNull(message = OBLIGATORIO) LocalDateTime fechaHora,
            @NotNull(message = OBLIGATORIO) TipoServicio tipoServicio) { }

    public record NuevaConsulta(
            @NotNull(message = OBLIGATORIO) TipoConsulta tipoConsulta,
            @NotBlank(message = SIN_TEXTO) @Size(max = 4000, message = MAX_CARACTERES) String diagnostico,
            @Size(max = 50, message = MAX_ELEMENTOS)
            List<@NotBlank(message = SIN_TEXTO) @Size(max = 200, message = MAX_CARACTERES) String> vacunasAplicadas,
            @Size(max = 50, message = MAX_ELEMENTOS)
            List<@NotBlank(message = SIN_TEXTO) @Size(max = 1000, message = MAX_CARACTERES) String> tratamientos,
            @Size(max = 50, message = MAX_ELEMENTOS)
            List<@NotBlank(message = SIN_TEXTO) @Size(max = 1000, message = MAX_CARACTERES) String> examenes,
            @Size(max = 4000, message = MAX_CARACTERES) String observaciones,
            LocalDate proximoControl) { }
}
