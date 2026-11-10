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
| `GET /api/veterinarios` | `200`, arreglo de `Veterinario` | Listar veterinarios. |
| `GET /api/veterinarios/{id}/disponibilidad?fecha=2026-11-20` | `200`, objeto `Disponibilidad` | Consultar las horas libres de un veterinario en la fecha indicada. |
| `POST /api/citas` | `201`, objeto `Cita` | Agendar y guardar `cita.agendada` para publicación. |
| `GET /api/citas?mascotaId={id}` | `200`, arreglo de `Cita` | Consultar las citas de una mascota. |
| `GET /api/citas?veterinarioId={id}&fecha=2026-11-20` | `200`, arreglo de `Cita` | Consultar la agenda de un veterinario para la fecha indicada. |
| `PATCH /api/citas/{id}/cancelar` | `200`, objeto `Cita` | Cancelar una cita. |
| `POST /api/citas/{id}/consulta` | `201`, objeto `RegistroHistorial` | Registrar la consulta, completar la cita y guardar `cita.completada` para publicación. |
| `GET /api/mascotas/{id}/historial` | `200`, arreglo de `RegistroHistorial` | Consultar el historial clínico de una mascota. |
| `GET /api/mascotas/{id}/proximas-vacunas` | `200`, arreglo de `VacunaPendiente` | Consultar las vacunas ficticias pendientes de una mascota. |

#### Formatos y validaciones

- Los identificadores son enteros positivos. `GET /api/citas` acepta `mascotaId` solo o `veterinarioId` junto con `fecha`.
- Las fechas de calendario usan ISO, por ejemplo `2026-11-20`. Las fechas con hora de negocio usan ISO local sin desplazamiento, por ejemplo `2026-11-20T09:30:00`, en `America/Bogota` por defecto. Se admiten los años 1 a 9999.
- Tipos de servicio `CONSULTA_GENERAL`, `VACUNACION`, `CONTROL` y `ESPECIALISTA`. Tipos de consulta `GENERAL` y `ESPECIALISTA`. Estados de cita `AGENDADA`, `COMPLETADA` y `CANCELADA`.
- La consulta exige diagnóstico. Vacunas aplicadas, tratamientos, exámenes, observaciones y próximo control son opcionales. El próximo control debe caer en la fecha de la consulta o después.

| Objeto de respuesta | Campos |
| --- | --- |
| `Veterinario` | `id`, `nombre`, `especialidad` |
| `Disponibilidad` | `veterinarioId`, `fecha`, `horarios` (arreglo de fechas con hora) |
| `Cita` | `id`, `mascotaId`, `veterinarioId`, `fechaHora`, `tipoServicio`, `estado`, `duracionMinutos` |
| `RegistroHistorial` | `id`, `citaId`, `mascotaId`, `veterinarioId`, `fecha` (fecha con hora), `tipoConsulta`, `diagnostico`, `vacunasAplicadas`, `tratamientos`, `examenes`, `observaciones`, `proximoControl` (fecha de calendario o nulo) |
| `VacunaPendiente` | `vacuna`, `fechaLimite` (fecha de calendario) |

Los errores tienen cuerpo `{"error": "...", "detalle": "..."}`.

| HTTP | Código | Cuándo ocurre |
| --- | --- | --- |
| `400` | `DATOS_INVALIDOS` | JSON, fechas, turno, parámetros o campos inválidos. |
| `404` | `RECURSO_NO_ENCONTRADO` | Recurso inexistente, incluida una ficha que Mascotas y Dueños responde con `404`. |
| `409` | `CONFLICTO` | Turno ocupado por veterinario o mascota, estado de cita incompatible con la operación, o dos solicitudes simultáneas sobre el mismo turno o cita. |
| `503` | `DEPENDENCIA_NO_DISPONIBLE` | Mascotas y Dueños no responde, falla o entrega una ficha inválida. |
| `500` | `ERROR_INTERNO` | Error no previsto al atender la operación. |

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
| `cita.completada` | Registrar la consulta y completar la cita. | `notificaciones.citas` | `citaId`, `mascotaNombre`, `duenoEmail`, `diagnostico`, `vacunasAplicadas`, `proximoControl`, `proximasVacunas` |
| `vacuna.proxima` | Detectar una vacuna próxima en la tarea `@Scheduled`. | `notificaciones.vacunas` | `mascotaId`, `mascotaNombre`, `duenoEmail`, `vacuna`, `fechaLimite` |

En `cita.completada`, `vacunasAplicadas` es un arreglo de textos, `proximasVacunas` un arreglo de objetos `VacunaPendiente` y `proximoControl` una fecha de calendario o nulo. El campo numérico `mascotaId` de `vacuna.proxima` es una extensión local del plan por confirmar con Pia.

Notificaciones debe enlazar `notificaciones.citas` con `cita.*` y `notificaciones.vacunas` con `vacuna.*`. Cada mensaje persistente tiene el cuerpo JSON en UTF-8 y un UUID estable en la propiedad AMQP `messageId`. La entrega es al menos una vez y el consumidor debe deduplicar por esa propiedad. Si RabbitMQ no está disponible o no hay cola enlazada, el evento queda pendiente en PostgreSQL para reintento.
