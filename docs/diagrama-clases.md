# Diagrama de clases UML

Diseño final implementado, un diagrama por servicio. Muestra las clases del dominio, los casos de uso, los puertos (interfaces del dominio) y los adaptadores de infraestructura que los implementan. Se omiten los controladores REST, los DTO y las entidades JPA, que solo traducen datos. Los patrones de cada diagrama están explicados con su código en [Sustentación del backend](sustentacion-backend.md).

Cómo leerlos: `<<interface>>` es un puerto del dominio; la flecha punteada con triángulo (`..|>`) significa "implementa"; la flecha continua (`-->`) significa "usa"; el rombo (`*--`) es composición. Los servicios no comparten clases: cada uno tiene su propia `Mascota` o `Especie` con solo los datos que necesita (base por servicio, [ADR-002](adr/ADR-002-base-de-datos-por-servicio.md)).

## Citas e Historial

Aquí están los dos patrones principales: **Builder** (`RegistroHistorialBuilder`) para armar el historial de una consulta y **Strategy** (`VacunacionStrategy`) para los esquemas de vacunación. Los puertos `MascotaClient` y `PublicadorEventos` son la inversión de dependencias: el dominio define la interfaz y la infraestructura la implementa con REST y con la outbox.

```mermaid
classDiagram
    direction TB

    class Cita {
        <<record>>
        Long id
        long mascotaId
        long veterinarioId
        LocalDateTime fechaHora
        TipoServicio tipoServicio
        EstadoCita estado
        int duracionMinutos
    }
    class Veterinario {
        <<record>>
        Long id
        String nombre
        String especialidad
        LocalTime horaInicio
        LocalTime horaFin
        +tieneHorario() boolean
    }
    class Mascota {
        <<record>>
        Long id
        String nombre
        Especie especie
        LocalDate fechaNacimiento
        String duenoNombre
        String duenoEmail
    }
    class RegistroHistorial {
        <<record>>
        Long id
        long citaId
        LocalDateTime fecha
        TipoConsulta tipoConsulta
        String diagnostico
        List~String~ vacunasAplicadas
        List~String~ tratamientos
        List~String~ examenes
        List~String~ cirugias
        String observaciones
        LocalDate proximoControl
    }
    class RegistroHistorialBuilder {
        +diagnostico(String) RegistroHistorialBuilder
        +vacunasAplicadas(List) RegistroHistorialBuilder
        +examenes(List) RegistroHistorialBuilder
        +cirugias(List) RegistroHistorialBuilder
        +proximoControl(LocalDate) RegistroHistorialBuilder
        +build() RegistroHistorial
    }
    class Consulta {
        <<interface>>
        +registrar(RegistroHistorialBuilder) RegistroHistorial
    }
    class ConsultaGeneral
    class ConsultaEspecialista

    class VacunacionStrategy {
        <<interface>>
        +aplica(Especie, int edadMeses) boolean
        +proximasVacunas(Mascota, List~RegistroHistorial~, LocalDate hoy) List~VacunaPendiente~
    }
    class EsquemaCachorro
    class EsquemaAdulto
    class EsquemaGeriatrico
    class SelectorEsquemaVacunacion {
        -List~VacunacionStrategy~ esquemas
        +proximasVacunas(Mascota, List, LocalDate) List~VacunaPendiente~
    }
    class VacunaPendiente {
        <<record>>
        String vacuna
        LocalDate fechaLimite
    }

    class CitaRepository {
        <<interface>>
        +guardar(Cita) Cita
        +agenda(long veterinarioId, LocalDate) List~Cita~
        +porMascota(long) List~Cita~
    }
    class HistorialRepository {
        <<interface>>
        +guardar(RegistroHistorial) RegistroHistorial
        +porMascota(long) List~RegistroHistorial~
    }
    class VeterinarioRepository {
        <<interface>>
        +listar() List~Veterinario~
    }
    class MascotaClient {
        <<interface>>
        +buscar(long id) Mascota
    }
    class PublicadorEventos {
        <<interface>>
        +citaAgendada(CitaAgendadaEvento)
        +citaCompletada(CitaCompletadaEvento)
        +vacunaProxima(VacunaProximaEvento)
    }

    class AgendaService {
        +disponibilidad(long, LocalDate) Disponibilidad
        +validarHorario(Veterinario, LocalDateTime)
        +cancelar(long id) Cita
    }
    class AgendarCitaUseCase {
        +ejecutar(mascotaId, veterinarioId, fechaHora, tipoServicio) Cita
    }
    class RegistrarConsultaUseCase {
        +ejecutar(long citaId, DatosConsulta) RegistroHistorial
    }
    class VacunasService {
        +proximas(long mascotaId) List~VacunaPendiente~
        +publicarRecordatorios(long mascotaId)
    }

    class MascotasRestClient
    class OutboxPublicadorEventos
    class JpaCitaRepository
    class JpaHistorialRepository

    Consulta <|.. ConsultaGeneral
    Consulta <|.. ConsultaEspecialista
    Consulta ..> RegistroHistorialBuilder : usa
    RegistroHistorialBuilder ..> RegistroHistorial : crea
    VacunacionStrategy <|.. EsquemaCachorro
    VacunacionStrategy <|.. EsquemaAdulto
    VacunacionStrategy <|.. EsquemaGeriatrico
    SelectorEsquemaVacunacion o-- VacunacionStrategy : esquemas
    VacunacionStrategy ..> VacunaPendiente : calcula

    AgendarCitaUseCase --> AgendaService
    AgendarCitaUseCase --> CitaRepository
    AgendarCitaUseCase --> MascotaClient
    AgendarCitaUseCase --> PublicadorEventos
    AgendaService --> VeterinarioRepository
    AgendaService --> CitaRepository
    RegistrarConsultaUseCase ..> Consulta : elige General o Especialista
    RegistrarConsultaUseCase --> CitaRepository
    RegistrarConsultaUseCase --> MascotaClient
    RegistrarConsultaUseCase --> SelectorEsquemaVacunacion
    RegistrarConsultaUseCase --> HistorialRepository
    RegistrarConsultaUseCase --> PublicadorEventos
    VacunasService --> SelectorEsquemaVacunacion
    VacunasService --> HistorialRepository
    VacunasService --> MascotaClient
    VacunasService --> PublicadorEventos

    MascotaClient <|.. MascotasRestClient : REST a Mascotas
    PublicadorEventos <|.. OutboxPublicadorEventos : outbox + RabbitMQ
    CitaRepository <|.. JpaCitaRepository
    HistorialRepository <|.. JpaHistorialRepository
    Cita ..> Veterinario : veterinarioId
    RegistroHistorial ..> Cita : citaId
```

## Mascotas y Dueños

Aplica **segregación de interfaces (ISP)**: leer y escribir están en puertos separados (`ConsultaMascotas` y `RegistroMascotas`, `ConsultaDuenos` y `RegistroDuenos`), así cada caso de uso depende solo de lo que usa.

```mermaid
classDiagram
    direction TB

    class Dueno {
        <<record>>
        Long id
        String nombre
        String documento
        String email
        String telefono
        String direccion
    }
    class Mascota {
        <<record>>
        Long id
        String nombre
        Especie especie
        String raza
        LocalDate fechaNacimiento
        BigDecimal peso
        String fotoUrl
        Long duenoId
        +conFoto(String) Mascota
    }
    class Especie {
        <<enumeration>>
        PERRO
        GATO
        AVE
        OTRO
    }
    class FotoMascota {
        <<record>>
        String tipoContenido
        byte[] datos
        +desde(byte[], String)$ FotoMascota
    }

    class ConsultaDuenos {
        <<interface>>
        +buscarPorId(long) Optional~Dueno~
        +buscarPorCorreo(String) Optional~Dueno~
        +listar() List~Dueno~
    }
    class RegistroDuenos {
        <<interface>>
        +guardar(Dueno) Dueno
    }
    class ConsultaMascotas {
        <<interface>>
        +buscarPorId(long) Optional~Mascota~
        +buscarPorDueno(long) List~Mascota~
    }
    class RegistroMascotas {
        <<interface>>
        +guardar(Mascota) Mascota
        +actualizar(Mascota) Mascota
    }
    class AlmacenFotosMascota {
        <<interface>>
        +actualizarFoto(long, FotoMascota, long)
        +buscarFoto(long) Optional~FotoGuardada~
    }

    class RegistrarDuenoUseCase
    class RegistrarMascotaUseCase
    class ActualizarMascotaUseCase
    class ConsultarMascotasUseCase {
        +ficha(long) FichaMascota
    }
    class SubirFotoMascotaUseCase

    class JpaDuenoRepository
    class JpaMascotaRepository
    class JpaAlmacenFotosMascota

    Dueno "1" -- "0..*" Mascota : tiene
    Mascota --> Especie
    Mascota ..> FotoMascota : foto

    RegistrarDuenoUseCase --> RegistroDuenos
    RegistrarDuenoUseCase --> ConsultaDuenos
    RegistrarMascotaUseCase --> ConsultaDuenos
    RegistrarMascotaUseCase --> RegistroMascotas
    ActualizarMascotaUseCase --> ConsultaMascotas
    ActualizarMascotaUseCase --> RegistroMascotas
    ConsultarMascotasUseCase --> ConsultaMascotas
    ConsultarMascotasUseCase --> ConsultaDuenos
    SubirFotoMascotaUseCase --> ConsultaMascotas
    SubirFotoMascotaUseCase --> AlmacenFotosMascota

    ConsultaDuenos <|.. JpaDuenoRepository
    RegistroDuenos <|.. JpaDuenoRepository
    ConsultaMascotas <|.. JpaMascotaRepository
    RegistroMascotas <|.. JpaMascotaRepository
    AlmacenFotosMascota <|.. JpaAlmacenFotosMascota
```

## Notificaciones

Aplica **Adapter**: los casos de uso envían por el puerto `EnviadorNotificaciones` y `CorreoSmtpAdapter` lo concreta con SMTP. `EnvioSinDuplicados` registra el `messageId` antes de enviar, así un evento repetido no genera otro correo.

```mermaid
classDiagram
    direction TB

    class Notificacion {
        <<record>>
        Long id
        String messageId
        TipoNotificacion tipo
        String destino
        String asunto
        String cuerpo
        Instant creadaEn
        Instant enviadaEn
    }
    class TipoNotificacion {
        <<enumeration>>
        CITA_AGENDADA
        CITA_COMPLETADA
        VACUNA_PROXIMA
    }
    class EnviadorNotificaciones {
        <<interface>>
        +enviar(String destino, String asunto, String cuerpo)
    }
    class NotificacionRepository {
        <<interface>>
        +registrarSiEsNueva(Notificacion) boolean
        +recientes(int) List~Notificacion~
    }
    class EnvioSinDuplicados {
        +enviar(messageId, TipoNotificacion, destino, MensajeCorreo) boolean
    }
    class PlantillasCorreo
    class NotificarCitaAgendadaUseCase {
        +ejecutar(String messageId, CitaAgendada) boolean
    }
    class NotificarCitaCompletadaUseCase
    class NotificarVacunaProximaUseCase
    class CitasListener
    class VacunasListener
    class CorreoSmtpAdapter {
        -JavaMailSender mailSender
        +enviar(String, String, String)
    }
    class JpaNotificacionRepository

    Notificacion --> TipoNotificacion
    CitasListener --> NotificarCitaAgendadaUseCase : cita.agendada
    CitasListener --> NotificarCitaCompletadaUseCase : cita.completada
    VacunasListener --> NotificarVacunaProximaUseCase : vacuna.proxima
    NotificarCitaAgendadaUseCase --> PlantillasCorreo
    NotificarCitaAgendadaUseCase --> EnvioSinDuplicados
    NotificarCitaCompletadaUseCase --> EnvioSinDuplicados
    NotificarVacunaProximaUseCase --> EnvioSinDuplicados
    EnvioSinDuplicados --> NotificacionRepository
    EnvioSinDuplicados --> EnviadorNotificaciones
    EnviadorNotificaciones <|.. CorreoSmtpAdapter : Adapter
    NotificacionRepository <|.. JpaNotificacionRepository
```

Las imágenes para el informe están en [img/](img/): `clases-citas.png`, `clases-mascotas.png` y `clases-notificaciones.png`.

