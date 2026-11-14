# Decisiones de backend de PetVet

## Arquitectura local

- Mascotas y Dueños, Citas e Historial y Notificaciones usan Java 21 y Spring Boot 3.5.16 y son módulos del POM raíz. El Maven Wrapper 3.3.4 descarga Maven 3.9.11. Cada servicio expone salud y Swagger. Docker Compose levanta un PostgreSQL 16 por servicio, RabbitMQ 3 y MailHog para pruebas.
- Cada servicio separa `domain`, `application`, `infrastructure` y `presentation`. El dominio usa Java sin Spring y define los puertos. Los adaptadores de `infrastructure` implementan esos puertos.
- En Citas, los puertos son de persistencia, consulta de mascotas y publicación de eventos. En Mascotas y Dueños, la consulta y el registro van en puertos separados (`ConsultaMascotas`, `RegistroMascotas`, `AlmacenFotosMascota`, `ConsultaDuenos` y `RegistroDuenos`), así que cada caso de uso depende solo de lo que usa. En Notificaciones, `EnviadorNotificaciones` es el puerto del patrón Adapter y `CorreoSmtpAdapter` lo implementa con SMTP.
- Citas obtiene la ficha de Mascotas y Dueños por REST. Los tres eventos salen por RabbitMQ hacia Notificaciones, que envía un correo por evento.

## Agenda

Cada veterinario atiende todos los días en su propia franja, guardada en la tabla `veterinarios` (`hora_inicio` y `hora_fin`, migración V7). En los datos iniciales, por ejemplo, la Dra. Valentina Ríos atiende de 07:00 a 13:00 y el Dr. Julián Herrera de 13:00 a 19:00. Los turnos duran 30 minutos por defecto y se cuentan desde la hora de inicio de cada veterinario, en `America/Bogota`. Solo se reservan turnos futuros que empiezan en una de esas divisiones y terminan dentro de la franja. La disponibilidad excluye turnos vencidos y solapamientos con citas que no estén canceladas.

La duración queda guardada con cada cita. PostgreSQL rechaza intervalos solapados de un mismo veterinario o una misma mascota, incluso con solicitudes concurrentes o si cambia la duración configurada. Dos turnos pueden tocarse en sus extremos. Cancelar una cita libera el turno y repetir esa cancelación devuelve la misma cita cancelada. Una cita completada no se cancela.

Antes de escribir, el servicio revisa el cruce con la misma regla de intervalos que usa PostgreSQL y responde `409` indicando si el ocupado es el veterinario o la mascota. Las restricciones de la base siguen siendo la garantía cuando dos solicitudes llegan a la vez.

La zona y la duración se configuran con `PETVET_AGENDA_ZONA` y `PETVET_AGENDA_DURACION_MINUTOS` en el proceso del servicio. En el Compose, la misma zona llega a Mascotas y Dueños, que la usa para rechazar fechas de nacimiento futuras, y a Notificaciones, que la usa para decir si una vacuna vence hoy. `PETVET_AGENDA_APERTURA` y `PETVET_AGENDA_CIERRE`, 08:00 y 18:00 por defecto, solo son el respaldo de un veterinario sin horario propio, que hoy no puede existir porque las dos columnas son obligatorias. Aun así, Citas las valida al arrancar: la duración debe ser positiva, de hasta 480 minutos, y dividir exactamente esa jornada general. Con 45 minutos, por ejemplo, el servicio no arranca.

## Transacciones y consulta a Mascotas

Agendar, registrar la consulta y generar recordatorios consultan la ficha en Mascotas y Dueños antes de abrir la transacción. Así, si ese servicio tarda o no responde, Citas devuelve `503` sin retener una conexión a PostgreSQL ni el bloqueo de la cita. La transacción solo cubre las escrituras: la cita o el historial, el cambio de estado y el evento en la outbox.

Al registrar la consulta, la cita se lee primero sin bloqueo para validar y, ya dentro de la transacción, se bloquea y se confirma que sigue agendada. Si dos registros de la misma consulta llegan a la vez, uno responde `201` y el otro `409`.

El historial carga sus listas de vacunas, tratamientos, exámenes y cirugías por lotes (`hibernate.default_batch_fetch_size`). Leer el historial de una mascota usa un número fijo de consultas SQL, sin importar cuántos registros tenga.

## Vacunación de demostración

Las reglas son un calendario académico simplificado con nombres de vacunas reales; no reemplazan el criterio del veterinario. `SelectorEsquemaVacunacion` toma la especie y la edad en meses cumplidos para elegir un esquema. Perros y gatos usan estas reglas. Para `AVE` y `OTRO` se devuelve una lista vacía. La vacuna combinada es `Polivalente` en perros y `Triple felina` en gatos. Cada vacuna se calcula por separado desde su última aplicación registrada, y el resultado sale en orden de vencimiento.

| Edad | Vacuna | Primera fecha límite | Refuerzo |
| --- | --- | --- | --- |
| Menos de 12 meses | Combinada | Nacimiento + 2 meses | Se omite si ya figura aplicada. |
| Menos de 12 meses | `Rabia` | Nacimiento + 3 meses | Se omite si ya figura aplicada. |
| 12 a 83 meses | Combinada y `Rabia` | Nacimiento + 12 meses | Última aplicación + 12 meses. |
| 84 meses en adelante | `Rabia` | Nacimiento + 84 meses | Última aplicación + 12 meses. |
| 84 meses en adelante | Combinada | Nacimiento + 84 meses | Última aplicación + 36 meses. |

Si una mascota no tiene ninguna aplicación registrada, la primera fecha límite puede quedar en el pasado y la vacuna aparece vencida. Para la demo, `db/demo/R__historial_demo.sql` carga un historial de ejemplo de Tobby, Misha y Kiwi (mascotas 1, 2 y 3 de los datos iniciales de Mascotas y Dueños). Tobby y Misha tienen vacunas aplicadas hace 9 a 11 meses, así que sus refuerzos vencen en los próximos meses; Kiwi es un ave, no tiene esquema de vacunas en la demo y solo trae una revisión general. Esa carga solo ocurre si Flyway incluye `classpath:db/demo`: el Compose de demo lo hace y el principal lo permite con `PETVET_CITAS_MIGRACIONES`.

`RecordatoriosVacunasJob` revisa las mascotas que tienen o tuvieron al menos una cita, aunque aún no tengan historial clínico. Las que nunca han tenido una cita no se revisan, porque Mascotas y Dueños no tiene una ruta que liste todas las mascotas. La tarea corre a las 08:00 en la zona configurada y cada minuto en el Compose de demo. Publica si el vencimiento cae entre hoy y hoy más siete días, incluidos los extremos. La clave mascota, vacuna y fecha límite evita generar el mismo recordatorio al volver a ejecutar la tarea. El cálculo puede mostrar una vacuna vencida en la consulta de próximas vacunas. El recordatorio aplica su propia ventana.

Si Mascotas y Dueños no responde, la tarea corta esa ejecución y retoma en la siguiente, en lugar de esperar el tiempo límite por cada mascota. Un error con una mascota puntual, por ejemplo una ficha eliminada, no impide revisar las demás. La tarea y el envío de la outbox usan hilos de planificación separados (`spring.task.scheduling.pool.size`), para que una no retrase a la otra.

## Publicación durable

`OutboxPublicadorEventos` guarda el evento en PostgreSQL dentro de la misma transacción de la cita o la consulta. `OutboxDispatcher` lo envía a RabbitMQ y marca el registro como enviado al recibir confirmación sin devolución. Si el broker está caído o no hay cola enlazada, el registro queda pendiente y se reintenta. El reintento reutiliza el `messageId` del evento. Cada ejecución envía hasta 50 eventos pendientes. Si el broker no responde, se detiene en el primero y espera a la siguiente ejecución. Si solo falta la cola de un evento, continúa con los demás.

La entrega es al menos una vez. Si RabbitMQ acepta un mensaje y el proceso se detiene antes de marcarlo como enviado en PostgreSQL, puede aparecer una copia con el mismo identificador. Notificaciones usa ese identificador para no enviar dos veces el mismo correo, como se explica en la sección siguiente. [Contrato de eventos](api.md#eventos-rabbitmq).

## Notificaciones

Notificaciones lee los eventos de Citas de sus colas `notificaciones.citas` y `notificaciones.vacunas` y envía un correo por evento. En una sola transacción registra el `messageId` en la tabla `notificaciones`, donde es único, y envía el correo de último. Si el `messageId` ya estaba, no envía nada. Si el envío falla, la transacción se deshace y el mensaje se puede reintentar. Solo queda un duplicado posible si la base falla al confirmar después de que el SMTP aceptó el correo. [Deduplicación](api.md#deduplicación).

Una falla pasajera, como el SMTP o la base caídos, se intenta 5 veces con esperas de 1, 2, 4 y 8 segundos, y después el mensaje pasa a su DLQ. Un mensaje que fallaría igual en cada intento, como uno sin `messageId`, con JSON inválido o con una dirección de correo mal escrita, pasa directo a la DLQ. [Procesamiento de los mensajes](api.md#procesamiento-de-los-mensajes) y [Reprocesar una DLQ](api.md#reprocesar-una-dlq).

## Pendientes de integración

- Sumar al frontend los campos que ya entrega la API: `documento` y `direccion` del dueño, `horaInicio` y `horaFin` del veterinario, y `cirugias` en la consulta y el historial. Hoy el frontend no los muestra ni los envía.
- Hacer el recorrido en equipo desde el frontend contra la API real, incluidos los correos en MailHog.
- Decidir si las mascotas que nunca han tenido una cita deben recibir recordatorios de vacunas. Para eso, Mascotas y Dueños necesitaría una ruta que liste todas las mascotas; hoy solo lista las de cada dueño.
