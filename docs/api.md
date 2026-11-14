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

## Notificaciones

`ms-notificaciones` escucha en el puerto `8083` y guarda sus datos en `postgres-notificaciones` (`5435`). Consume los eventos de Citas y envía un correo por evento al SMTP configurado. En local los correos llegan a MailHog, en `http://localhost:8025`.

### Colas y enlaces

Al arrancar, el servicio declara el exchange `petvet.events` igual que Citas: `topic`, durable y sin `autoDelete`. También declara sus colas y sus DLQ.

| Cola | Enlace desde `petvet.events` | Mensajes rechazados | DLQ |
| --- | --- | --- | --- |
| `notificaciones.citas` | `cita.*` | `petvet.notificaciones.dlx` con la clave `notificaciones.citas.dlq` | `notificaciones.citas.dlq` |
| `notificaciones.vacunas` | `vacuna.*` | `petvet.notificaciones.dlx` con la clave `notificaciones.vacunas.dlq` | `notificaciones.vacunas.dlq` |

Las cuatro colas son durables. `petvet.notificaciones.dlx` es un exchange `direct` durable y enlaza cada DLQ con su propio nombre. Si ya existe una cola con el mismo nombre y otros argumentos, por ejemplo creada a mano sin DLQ, RabbitMQ rechaza la declaración con `PRECONDITION_FAILED`. En ese caso hay que borrar la cola antes de arrancar el servicio.

### Procesamiento de los mensajes

- Cada cola tiene un solo listener. El listener recibe el mensaje sin convertir y elige el caso de uso por la routing key: `cita.agendada`, `cita.completada` o `vacuna.proxima`. Si el mensaje se reenvió desde una DLQ, usa la propiedad `type`, que Citas llena con la misma clave.
- El cuerpo se lee como JSON y se ignoran los campos desconocidos. `examenes` en `cita.completada` es opcional y, si llega, se lista en el resumen. `duenoNombre` es opcional en `cita.completada` y `vacuna.proxima`. Cuando el evento lo trae, el correo saluda por nombre.
- Una clave que no genera notificación, como `cita.cancelada`, se registra en el log y se descarta.
- Un mensaje sin `messageId`, con JSON inválido o sin un campo obligatorio se rechaza sin reencolar. Pasa directo a su DLQ, sin reintentos.
- Una falla pasajera, como el SMTP o la base caídos, se intenta 3 veces en total, con esperas de 1 s y 2 s. Si la falla continúa, el mensaje pasa a la DLQ y los demás mensajes siguen su curso.
- Para reprocesar un mensaje, se publica de nuevo en su cola con las mismas propiedades, por el exchange por defecto y con el nombre de la cola como clave. Con el plugin `rabbitmq_shovel_management` activo, el panel de RabbitMQ lo hace con "Move messages". Como el mensaje conserva su `messageId`, reprocesarlo no repite un correo ya enviado.

### Deduplicación

La tabla `notificaciones` guarda una fila por correo enviado.

| Columna | Contenido |
| --- | --- |
| `id` | Identificador generado. |
| `message_id` | Propiedad AMQP `messageId`, que es el UUID de la outbox de Citas. Es única. |
| `tipo` | `CITA_AGENDADA`, `CITA_COMPLETADA` o `VACUNA_PROXIMA`. |
| `destino`, `asunto`, `cuerpo` | Correo enviado. |
| `creada_en`, `enviada_en` | Instantes con zona horaria. |

Todo ocurre en una transacción. Primero se inserta la fila con `INSERT ... ON CONFLICT (message_id) DO NOTHING`. Si el `messageId` ya existía, el mensaje se confirma sin enviar nada. Si es nuevo, el correo se envía de último. Si el SMTP falla, la transacción se deshace y el mensaje se reintenta. Solo puede quedar un duplicado si la base falla al confirmar después de que el SMTP aceptó el correo.

### Correos

Los correos son texto plano en UTF-8. El remitente por defecto es `PetVet <notificaciones@petvet.local>` y se cambia con `PETVET_NOTIFICACIONES_REMITENTE`. Las fechas se escriben en español y la hora usa el formato de 24 horas. `PETVET_NOTIFICACIONES_ZONA` fija la zona con que se compara la fecha de las vacunas; por defecto es `America/Bogota`.

| Evento | Asunto |
| --- | --- |
| `cita.agendada` | `Confirmación de la cita de {mascota}` |
| `cita.completada` | `Resumen de la consulta de {mascota}` |
| `vacuna.proxima` | `Recordatorio: vacuna {vacuna} de {mascota}` |

Ejemplo de `cita.agendada` con datos sintéticos.

```text
Hola, Laura Méndez:

La cita de vacunación de Tobby con la Dra. Paula Castaño quedó para el viernes 20 de noviembre de 2026 a las 09:30.

Si no puede asistir, cancele la cita en PetVet para liberar el turno.

Equipo PetVet
```

Ejemplo de `cita.completada` con exámenes. Sin exámenes, la línea de exámenes no aparece. Si faltan datos, el correo dice `Vacunas aplicadas: ninguna`, `Próximo control: sin control programado` o `Próximas vacunas: ninguna pendiente`.

```text
Hola:

Este es el resumen de la consulta de Tobby.

Diagnóstico: Otitis leve
Vacunas aplicadas: Rabia
Exámenes registrados en la consulta: Hemograma, Otoscopia
Próximo control: viernes 4 de diciembre de 2026

Próximas vacunas:
- Polivalente: antes del domingo 13 de diciembre de 2026
- Rabia: antes del sábado 20 de noviembre de 2027

Equipo PetVet
```

Ejemplo de `vacuna.proxima`. Si la fecha límite es hoy, la frase dice `vence hoy, ...`, y si ya pasó, `venció el ...`.

```text
Hola:

La vacuna Polivalente de su mascota Max va a vencer pronto. Aplíquela antes del lunes 16 de noviembre de 2026.

Puede agendar la cita de vacunación en PetVet.

Equipo PetVet
```

### Consulta de lo enviado

| Método y ruta | Uso |
| --- | --- |
| `GET /api/notificaciones?limite=20` | Listar las últimas notificaciones enviadas, de la más reciente a la más antigua. `limite` va de 1 a 100 y vale 20 por defecto. |
| `GET /actuator/health` | Consultar la salud del servicio. |
| `GET /v3/api-docs` | Consultar el documento OpenAPI en JSON. |
| `GET /swagger-ui/index.html` | Abrir la interfaz de Swagger. |

Ejemplo de respuesta de `GET /api/notificaciones?limite=1`.

```json
[
  {
    "id": 2,
    "messageId": "6b1f3c2e-9d7a-4f0e-8c55-0a4b2d9e7f13",
    "tipo": "VACUNA_PROXIMA",
    "destino": "santiago.rojas@correo.com",
    "asunto": "Recordatorio: vacuna Polivalente de Max",
    "cuerpo": "Hola:\n\nLa vacuna Polivalente de su mascota Max va a vencer pronto. Aplíquela antes del lunes 16 de noviembre de 2026.\n\nPuede agendar la cita de vacunación en PetVet.\n\nEquipo PetVet\n",
    "creadaEn": "2026-11-13T17:00:00.412318Z",
    "enviadaEn": "2026-11-13T17:00:00.415027Z"
  }
]
```

Un límite fuera de rango responde `400` con el formato de error del servicio.

```json
{
  "error": "DATOS_INVALIDOS",
  "detalle": "El límite debe estar entre 1 y 100"
}
```

La salud refleja PostgreSQL y RabbitMQ. El chequeo de correo está desactivado para que una caída de MailHog no marque el servicio como caído: en ese caso los correos se reintentan y pasan a la DLQ.
