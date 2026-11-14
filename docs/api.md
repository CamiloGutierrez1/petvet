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

### Mascotas y Dueños implementado

Mascotas y Dueños atiende en el puerto `8081`, con las mismas rutas técnicas y el mismo origen permitido que Citas e Historial.

| Método y ruta | Cuerpo | Respuesta de éxito | Errores | Uso |
| --- | --- | --- | --- | --- |
| `POST /api/duenos` | `NuevoDueno` | `201`, objeto `Dueno` | `400`, `409` | Registrar un dueño. |
| `GET /api/duenos` | — | `200`, arreglo de `Dueno` ordenado por `id` | — | Listar los dueños para el selector de perfil y la entrada por correo. |
| `GET /api/duenos/{id}` | — | `200`, objeto `Dueno` | `400`, `404` | Consultar los datos de un dueño. |
| `POST /api/duenos/{id}/mascotas` | `DatosMascota` | `201`, objeto `Mascota` | `400`, `404` | Registrar una mascota del dueño de la ruta. |
| `GET /api/duenos/{id}/mascotas` | — | `200`, arreglo de `Mascota` ordenado por `id` | `400`, `404` | Listar las mascotas de un dueño. |
| `GET /api/mascotas/{id}` | — | `200`, objeto `FichaMascota` | `400`, `404` | Consultar la ficha de una mascota con el nombre y el correo del dueño. La consumen el frontend y `ms-citas-historial`. |
| `PUT /api/mascotas/{id}` | `DatosMascota` | `200`, objeto `Mascota` | `400`, `404` | Editar una mascota. Cambia solo los cinco campos del cuerpo y conserva la foto y el dueño. |
| `POST /api/mascotas/{id}/foto` | `multipart/form-data` con el campo `foto` | `200`, objeto `Mascota` con `fotoUrl` absoluta | `400`, `404` | Subir o reemplazar la foto de una mascota. |
| `GET /api/mascotas/{id}/foto` | — | `200`, la imagen con su `Content-Type` | `400`, `404` | Descargar la foto subida. Es la dirección de `fotoUrl`. |

Las respuestas `201` incluyen la cabecera `Location`, por ejemplo `/api/mascotas/4`. Los identificadores de ruta son enteros positivos; otro valor responde `400`.

#### Campos de Mascotas y Dueños

Cuerpo `NuevoDueno` de `POST /api/duenos`.

| Campo | Tipo | Obligatorio | Reglas |
| --- | --- | --- | --- |
| `nombre` | texto | Sí | Hasta 100 caracteres. |
| `documento` | texto | No | Hasta 30 caracteres. |
| `email` | texto | Sí | Hasta 254 caracteres, con la forma `ana@correo.com`. |
| `telefono` | texto | No | Solo dígitos, entre 7 y 10. |
| `direccion` | texto | No | Hasta 200 caracteres. |

Cuerpo `DatosMascota` de `POST /api/duenos/{id}/mascotas` y de `PUT /api/mascotas/{id}`.

| Campo | Tipo | Obligatorio | Reglas |
| --- | --- | --- | --- |
| `nombre` | texto | Sí | Hasta 60 caracteres. |
| `especie` | texto | Sí | `PERRO`, `GATO`, `AVE` u `OTRO`. |
| `raza` | texto | Sí | Hasta 60 caracteres. |
| `fechaNacimiento` | fecha de calendario | Sí | ISO, por ejemplo `2021-04-18`. No puede ser futura en `America/Bogota` ni anterior a `1900-01-01`. |
| `peso` | número | Sí | Kilogramos, mayor que 0, con hasta 4 enteros y 3 decimales, por ejemplo `31.5` o `0.035`. |

| Objeto de respuesta | Campos |
| --- | --- |
| `Dueno` | `id`, `nombre`, `documento` (texto o nulo), `email`, `telefono` (texto o nulo), `direccion` (texto o nulo) |
| `Mascota` | `id`, `nombre`, `especie`, `raza`, `fechaNacimiento`, `peso`, `fotoUrl` (texto o nulo), `duenoId` |
| `FichaMascota` | Los campos de `Mascota` más `duenoNombre` y `duenoEmail`. El ejemplo está en el contrato con Citas, más abajo. |

- El correo se guarda sin espacios y en minúsculas, y debe cumplir la expresión con la que Citas valida la ficha, `[^\s@]+@[^\s@]+\.[^\s@]+`. Es único sin distinguir mayúsculas: repetirlo responde `409`.
- Los textos se guardan sin espacios al inicio ni al final, y los opcionales vacíos o ausentes, como nulo.
- El cuerpo de una mascota no lleva `fotoUrl` ni `duenoId`. Al registrar, el dueño sale de la ruta; el `PUT` conserva la foto y el dueño aunque el cuerpo los traiga.
- `peso` se responde sin ceros sobrantes: `31.5`, `0.04` o `100`.
- Los datos de ejemplo son los de los mocks del frontend: Laura Méndez (dueño `1`) con Tobby y Misha (mascotas `1` y `2`), y Santiago Rojas (dueño `2`) con Kiwi (mascota `3`). Los registros nuevos empiezan en el dueño `3` y en la mascota `4`.

#### Fotos de las mascotas

- `POST /api/mascotas/{id}/foto` recibe la imagen en el campo `foto` de un formulario `multipart/form-data`. Admite JPG, PNG o WebP de hasta 5 MB y comprueba el formato con los primeros bytes del archivo, no con su nombre ni con el tipo que declara.
- La foto se guarda en PostgreSQL con su tipo de contenido y una versión que cambia en cada subida. La base de datos nunca guarda la URL.
- La respuesta trae `fotoUrl` absoluta, por ejemplo `http://localhost:8081/api/mascotas/4/foto?v=1763078400000`. La base sale de `PETVET_FOTOS_URL_BASE`, por defecto `http://localhost:8081`, que es la dirección con la que el navegador llega al servicio. La versión evita que el navegador muestre la foto anterior.
- Las mascotas de ejemplo tienen `fotoUrl` relativa al frontend, como `/mascotas/tobby.jpg`, hasta que se les sube una foto.
- `GET /api/mascotas/{id}/foto` responde `404` si la mascota no tiene una foto subida.
- La foto se sirve con `Cache-Control: no-cache` y una `ETag` igual a su versión. El navegador la guarda y la revalida con `If-None-Match`: si no cambió, recibe `304` sin los bytes.

#### Errores de Mascotas y Dueños

Los errores tienen el mismo cuerpo que en Citas, `{"error": "...", "detalle": "..."}`, con el detalle en español. Siempre se responden como `application/json`, aunque la cabecera `Accept` pida otro formato.

| HTTP | Código | Cuándo ocurre |
| --- | --- | --- |
| `400` | `DATOS_INVALIDOS` | Campos inválidos, JSON mal formado, especie desconocida, fecha de nacimiento futura, identificador de ruta inválido, o una foto que falta, no es JPG, PNG o WebP, o pesa más de 5 MB. |
| `404` | `RECURSO_NO_ENCONTRADO` | Dueño, mascota, foto subida o ruta inexistente. |
| `409` | `CONFLICTO` | Correo ya registrado, con el detalle `Ya existe un dueño registrado con ese correo.`, también cuando llegan dos registros simultáneos con el mismo correo. Si la base rechaza los datos por otra restricción, el detalle es genérico y no menciona el correo. |
| `405` | `METODO_NO_PERMITIDO` | Método HTTP que la ruta no admite. |
| `406` | `RESPUESTA_NO_ACEPTABLE` | La cabecera `Accept` no admite el formato de la respuesta. |
| `415` | `CONTENIDO_NO_SOPORTADO` | Cuerpo que no es JSON, o foto que no llega como `multipart/form-data`. |
| `500` | `ERROR_INTERNO` | Error no previsto al atender la operación. |

#### Ejemplo de `POST /api/duenos`

```json
{
  "nombre": "Ana Prueba",
  "documento": "1000000003",
  "email": "ana.prueba@correo.com",
  "telefono": "3001234567",
  "direccion": "Calle 1 # 2-3, Bogotá"
}
```

#### Ejemplo de `POST /api/duenos/{id}/mascotas` y `PUT /api/mascotas/{id}`

```json
{
  "nombre": "Max",
  "especie": "PERRO",
  "raza": "Beagle",
  "fechaNacimiento": "2025-06-01",
  "peso": 12.5
}
```

Respuesta `201` de ese registro para el dueño `3`, en una base recién creada.

```json
{
  "id": 4,
  "nombre": "Max",
  "especie": "PERRO",
  "raza": "Beagle",
  "fechaNacimiento": "2025-06-01",
  "peso": 12.5,
  "fotoUrl": null,
  "duenoId": 3
}
```

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

## Contrato que Citas consume de Mascotas y Dueños

Citas lee `GET /api/mascotas/{id}` en Mascotas y Dueños, que responde la ficha plana con estos diez campos. Citas exige el `id` numérico solicitado, `nombre`, `especie` (`PERRO`, `GATO`, `AVE` u `OTRO`), `fechaNacimiento` en ISO sin fecha futura en `America/Bogota`, `duenoNombre` y `duenoEmail` con la forma `ana@correo.com`. Ignora los demás campos (`raza`, `peso`, `fotoUrl` y `duenoId`), que usa el frontend.

Ficha de Tobby, de los datos de ejemplo.

```json
{
  "id": 1,
  "nombre": "Tobby",
  "especie": "PERRO",
  "raza": "Golden Retriever",
  "fechaNacimiento": "2021-04-18",
  "peso": 31.5,
  "fotoUrl": "/mascotas/tobby.jpg",
  "duenoId": 1,
  "duenoNombre": "Laura Méndez",
  "duenoEmail": "laura.mendez@correo.com"
}
```

- Una mascota inexistente responde `404` con `RECURSO_NO_ENCONTRADO`, y Citas responde también `404`. Si Mascotas y Dueños no responde o falla, Citas responde `503`.
- `fotoUrl` vale nulo si la mascota no tiene foto. Es una URL relativa al frontend en los datos de ejemplo y absoluta si se le subió una foto.
- El adaptador de Citas acepta también el objeto anidado `dueno`, pero Mascotas y Dueños responde siempre el formato plano.

## Eventos RabbitMQ

Citas implementa la publicación en el exchange durable `petvet.events` de tipo `topic`. Las colas de integración son responsabilidad de Pia. El script de demo crea colas propias para comprobar la publicación.

| Clave de enrutamiento | Cuándo se publica | Cola | Campos del mensaje |
| --- | --- | --- | --- |
| `cita.agendada` | Agendar una cita. | `notificaciones.citas` | `citaId`, `mascotaNombre`, `duenoNombre`, `duenoEmail`, `veterinarioNombre`, `fechaHora`, `tipoServicio` |
| `cita.completada` | Registrar la consulta y completar la cita. | `notificaciones.citas` | `citaId`, `mascotaNombre`, `duenoEmail`, `diagnostico`, `vacunasAplicadas`, `proximoControl`, `proximasVacunas` |
| `vacuna.proxima` | Detectar una vacuna próxima en la tarea `@Scheduled`. | `notificaciones.vacunas` | `mascotaId`, `mascotaNombre`, `duenoEmail`, `vacuna`, `fechaLimite` |

En `cita.completada`, `vacunasAplicadas` es un arreglo de textos, `proximasVacunas` un arreglo de objetos `VacunaPendiente` y `proximoControl` una fecha de calendario o nulo. El campo numérico `mascotaId` de `vacuna.proxima` es una extensión local del plan por confirmar con Pia.

Notificaciones debe enlazar `notificaciones.citas` con `cita.*` y `notificaciones.vacunas` con `vacuna.*`. Cada mensaje persistente tiene el cuerpo JSON en UTF-8 y un UUID estable en la propiedad AMQP `messageId`. La entrega es al menos una vez y el consumidor debe deduplicar por esa propiedad. Si RabbitMQ no está disponible o no hay cola enlazada, el evento queda pendiente en PostgreSQL para reintento.
