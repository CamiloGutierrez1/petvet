# Contratos de API y eventos de PetVet

Este documento reúne el contrato local de Citas e Historial y las rutas de Mascotas y Dueños previstas en las páginas 5 a 9 del plan general. El equipo debe ratificar los contratos antes de conectar el frontend y los consumidores.

## Servicios y puertos

Los puertos de la tabla corresponden al acceso desde la máquina local.

| Servicio | Puertos | Base de datos | Responsable |
| --- | --- | --- | --- |
| `frontend` | `5173` | — | Camilo y Andrés Daza |
| `ms-mascotas-duenos` | `8081` | `postgres-mascotas` en `5433` | Pia |
| `ms-citas-historial` | `8082` | `postgres-citas` en `5434` | Jersson |
| `ms-notificaciones` | `8083` | `postgres-notificaciones` en `5435` | Pia |
| RabbitMQ | AMQP `5672`, panel `15672` | — | Jersson |
| MailHog | SMTP `1025`, bandeja `8025` | — | Jersson |

## Rutas disponibles en el arranque local

Citas e Historial expone estas rutas técnicas en el puerto `8082`.

| Método y ruta | Uso |
| --- | --- |
| `GET /actuator/health` | Consultar la salud del servicio. |
| `GET /v3/api-docs` | Consultar el documento OpenAPI en JSON. |
| `GET /swagger-ui/index.html` | Abrir la interfaz de Swagger. |

El origen permitido por defecto es `http://localhost:5173`.

## API REST

El plan prevé un selector de perfil de dueño o veterinario en el frontend, que conserva el identificador elegido y funciona sin inicio de sesión.

### Mascotas y Dueños pendiente de integración

Estas rutas corresponden al plan. Pia implementa el módulo.

| Método y ruta | Uso | Consumidor previsto |
| --- | --- | --- |
| `POST /api/duenos` | Registrar un dueño. | Camilo |
| `GET /api/duenos` | Listar dueños para el selector de perfil. | Camilo |
| `GET /api/duenos/{id}` | Consultar los datos de un dueño. | Camilo |
| `POST /api/duenos/{id}/mascotas` | Registrar una mascota de un dueño. | Camilo |
| `GET /api/duenos/{id}/mascotas` | Listar las mascotas de un dueño. | Camilo |
| `GET /api/mascotas/{id}` | Consultar la ficha de una mascota, incluidos el nombre y el correo del dueño. | Camilo, Andrés Daza y `ms-citas-historial` |
| `PUT /api/mascotas/{id}` | Editar una mascota. | Camilo |
| `POST /api/mascotas/{id}/foto` | Subir una foto mediante `multipart/form-data`. | Camilo |

### Citas e Historial implementado

| Método y ruta | Respuesta de éxito | Uso |
| --- | --- | --- |
| `GET /api/veterinarios` | `200`, arreglo de `Veterinario` | Listar veterinarios con su horario de atención. |
| `GET /api/veterinarios/{id}/disponibilidad?fecha=2026-11-20` | `200`, objeto `Disponibilidad` | Consultar los turnos libres de un veterinario en la fecha indicada, dentro de su horario de atención. |
| `POST /api/citas` | `201`, objeto `Cita` | Agendar y guardar `cita.agendada` para publicación. |
| `GET /api/citas/{id}` | `200`, objeto `Cita` | Consultar una cita con su estado actual, por ejemplo para registrar su consulta. |
| `GET /api/citas?mascotaId={id}` | `200`, arreglo de `Cita` | Consultar las citas de una mascota, de la más reciente a la más antigua. |
| `GET /api/citas?veterinarioId={id}` | `200`, arreglo de `Cita` | Consultar todas las citas de un veterinario, de la más reciente a la más antigua, incluidas las canceladas. Sirve para armar su lista de pacientes. |
| `GET /api/citas?veterinarioId={id}&fecha=2026-11-20` | `200`, arreglo de `Cita` | Consultar la agenda de un veterinario para la fecha indicada, en orden de hora. |
| `PATCH /api/citas/{id}/cancelar` | `200`, objeto `Cita` | Cancelar una cita. |
| `POST /api/citas/{id}/consulta` | `201`, objeto `RegistroHistorial` | Registrar la consulta, completar la cita y guardar `cita.completada` para publicación. |
| `GET /api/mascotas/{id}/historial` | `200`, arreglo de `RegistroHistorial` | Consultar el historial clínico de una mascota. |
| `GET /api/mascotas/{id}/proximas-vacunas` | `200`, arreglo de `VacunaPendiente` | Consultar las vacunas pendientes de una mascota, en orden de vencimiento. |

#### Formatos y validaciones

- Los identificadores son enteros positivos. `GET /api/citas` acepta `mascotaId` solo, `veterinarioId` solo o `veterinarioId` junto con `fecha`. Otra combinación responde `400`, y un veterinario inexistente, `404`.
- Las vacunas que calcula Citas son `Polivalente` (perros), `Triple felina` (gatos) y `Rabia`. Para que una vacuna aplicada cuente en el cálculo, la consulta debe registrarla con ese mismo nombre; no importan las mayúsculas.
- Las fechas de calendario usan ISO, por ejemplo `2026-11-20`. Las fechas con hora de negocio usan ISO local sin desplazamiento, por ejemplo `2026-11-20T09:30:00`, en `America/Bogota` por defecto. Se admiten los años 1 a 9999.
- Tipos de servicio `CONSULTA_GENERAL`, `VACUNACION`, `CONTROL` y `ESPECIALISTA`. Tipos de consulta `GENERAL` y `ESPECIALISTA`. Estados de cita `AGENDADA`, `COMPLETADA` y `CANCELADA`.
- Cada veterinario atiende todos los días de `horaInicio` a `horaFin`, en formato `HH:mm`. Los turnos duran 30 minutos por defecto (`PETVET_AGENDA_DURACION_MINUTOS`) y se cuentan desde `horaInicio`. Una cita debe empezar en uno de esos turnos y terminar a más tardar en `horaFin`; si no, la respuesta es `400` con la franja del veterinario en el detalle, por ejemplo `La cita debe quedar dentro del horario del veterinario, de 07:00 a 13:00`. Al agendar, un veterinario inexistente responde `404` antes de revisar la hora. `PETVET_AGENDA_APERTURA` y `PETVET_AGENDA_CIERRE` solo se aplican a un veterinario sin horario propio.
- La consulta exige diagnóstico. Vacunas aplicadas, tratamientos, exámenes, cirugías, observaciones y próximo control son opcionales. El próximo control debe caer en la fecha de la consulta o después.
- Cada lista de la consulta admite hasta 50 textos no vacíos: hasta 200 caracteres por vacuna y hasta 1000 por tratamiento, examen o cirugía. Una lista que no se envía se guarda vacía. Las cirugías son una lista del historial, como los tratamientos, y no un tipo de servicio ni de consulta.

| Objeto de respuesta | Campos |
| --- | --- |
| `Veterinario` | `id`, `nombre`, `especialidad`, `horaInicio` y `horaFin` (hora `HH:mm`) |
| `Disponibilidad` | `veterinarioId`, `fecha`, `horarios` (arreglo de fechas con hora) |
| `Cita` | `id`, `mascotaId`, `veterinarioId`, `fechaHora`, `tipoServicio`, `estado`, `duracionMinutos` |
| `RegistroHistorial` | `id`, `citaId`, `mascotaId`, `veterinarioId`, `fecha` (fecha con hora), `tipoConsulta`, `diagnostico`, `vacunasAplicadas`, `tratamientos`, `examenes`, `cirugias` (arreglos de textos, vacíos si no hay datos), `observaciones`, `proximoControl` (fecha de calendario o nulo) |
| `VacunaPendiente` | `vacuna`, `fechaLimite` (fecha de calendario) |

Los errores tienen cuerpo `{"error": "...", "detalle": "..."}`, con el detalle en español. Si falla la validación de un campo, el detalle empieza con su nombre, por ejemplo `cirugias[1] no puede estar vacío`.

| HTTP | Código | Cuándo ocurre |
| --- | --- | --- |
| `400` | `DATOS_INVALIDOS` | JSON, fechas, turno fuera del horario del veterinario, parámetros o campos inválidos. |
| `404` | `RECURSO_NO_ENCONTRADO` | Recurso inexistente, incluida una ficha que Mascotas y Dueños responde con `404`. |
| `409` | `CONFLICTO` | Turno ocupado por veterinario o mascota, estado de cita incompatible con la operación, o dos solicitudes simultáneas sobre el mismo turno o cita. |
| `503` | `DEPENDENCIA_NO_DISPONIBLE` | Mascotas y Dueños no responde, falla o entrega una ficha inválida. |
| `500` | `ERROR_INTERNO` | Error no previsto al atender la operación. |

#### Ejemplo de respuesta de `GET /api/veterinarios`

```json
[
  { "id": 1, "nombre": "Dra. Paula Castaño", "especialidad": "Medicina general", "horaInicio": "08:00", "horaFin": "18:00" },
  { "id": 2, "nombre": "Dr. Andrés Villamil", "especialidad": "Dermatología", "horaInicio": "09:00", "horaFin": "17:00" },
  { "id": 3, "nombre": "Dra. Valentina Ríos", "especialidad": "Cirugía", "horaInicio": "07:00", "horaFin": "13:00" },
  { "id": 4, "nombre": "Dr. Julián Herrera", "especialidad": "Odontología", "horaInicio": "13:00", "horaFin": "19:00" },
  { "id": 5, "nombre": "Dra. Sofía Cárdenas", "especialidad": "Medicina felina", "horaInicio": "10:00", "horaFin": "16:00" }
]
```

#### Ejemplo de `POST /api/citas`

Usa un turno futuro devuelto por disponibilidad al ejecutar el ejemplo.

```json
{
  "mascotaId": 3,
  "veterinarioId": 1,
  "fechaHora": "2026-11-20T09:30:00",
  "tipoServicio": "VACUNACION"
}
```

#### Ejemplo de `POST /api/citas/{id}/consulta`

```json
{
  "tipoConsulta": "GENERAL",
  "diagnostico": "Otitis leve",
  "vacunasAplicadas": ["Rabia"],
  "tratamientos": ["Gotas óticas 7 días"],
  "examenes": [],
  "cirugias": [],
  "observaciones": "Control en 2 semanas",
  "proximoControl": "2026-12-04"
}
```

## Contrato provisional que Citas consume de Mascotas y Dueños

Citas lee `GET /api/mascotas/{id}` en el servicio de Pia. La respuesta debe incluir el `id` numérico solicitado, `nombre`, `especie` (`PERRO`, `GATO`, `AVE` u `OTRO`), `fechaNacimiento` en ISO sin fecha futura, nombre y correo del dueño. El adaptador acepta el formato plano o el objeto `dueno`.

Formato plano con datos sintéticos.

```json
{
  "id": 3,
  "nombre": "Mascota demo",
  "especie": "PERRO",
  "fechaNacimiento": "2026-09-15",
  "duenoNombre": "Dueño demo",
  "duenoEmail": "demo@example.test"
}
```

Formato anidado con los mismos datos.

```json
{
  "id": 3,
  "nombre": "Mascota demo",
  "especie": "PERRO",
  "fechaNacimiento": "2026-09-15",
  "dueno": {
    "nombre": "Dueño demo",
    "email": "demo@example.test"
  }
}
```

## Eventos RabbitMQ

Citas implementa la publicación en el exchange durable `petvet.events` de tipo `topic`. Las colas de integración son responsabilidad de Pia. El script de demo crea colas propias para comprobar la publicación.

| Clave de enrutamiento | Cuándo se publica | Cola | Campos del mensaje |
| --- | --- | --- | --- |
| `cita.agendada` | Agendar una cita. | `notificaciones.citas` | `citaId`, `mascotaNombre`, `duenoNombre`, `duenoEmail`, `veterinarioNombre`, `fechaHora`, `tipoServicio` |
| `cita.completada` | Registrar la consulta y completar la cita. | `notificaciones.citas` | `citaId`, `mascotaNombre`, `duenoEmail`, `diagnostico`, `vacunasAplicadas`, `proximoControl`, `proximasVacunas`, `examenes` |
| `vacuna.proxima` | Detectar una vacuna próxima en la tarea `@Scheduled`. | `notificaciones.vacunas` | `mascotaId`, `mascotaNombre`, `duenoEmail`, `vacuna`, `fechaLimite` |

En `cita.completada`, `vacunasAplicadas` es un arreglo de textos, `proximasVacunas` un arreglo de objetos `VacunaPendiente` y `proximoControl` una fecha de calendario o nulo. `examenes` es un arreglo con los nombres de los exámenes registrados en la consulta, vacío si no se registró ninguno; con él Notificaciones arma el aviso de exámenes sin un evento aparte. Un mensaje publicado antes de que existiera este campo no lo trae, así que el consumidor debe tomarlo como lista vacía. El campo numérico `mascotaId` de `vacuna.proxima` es una extensión local del plan.

Notificaciones debe enlazar `notificaciones.citas` con `cita.*` y `notificaciones.vacunas` con `vacuna.*`. Cada mensaje persistente tiene el cuerpo JSON en UTF-8 y un UUID estable en la propiedad AMQP `messageId`. La entrega es al menos una vez y el consumidor debe deduplicar por esa propiedad. Si RabbitMQ no está disponible o no hay cola enlazada, el evento queda pendiente en PostgreSQL para reintento.
