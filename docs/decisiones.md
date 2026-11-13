# Decisiones de backend de PetVet

## Arquitectura local

- Citas e Historial usa Java 21, Spring Boot 3.5.16 y Maven Wrapper 3.9.11, y expone salud y Swagger. El POM raíz queda listo para sumar los demás servicios como módulos. Docker Compose levanta un PostgreSQL 16 por servicio, RabbitMQ 3 y MailHog para pruebas.
- Citas e Historial separa `domain`, `application`, `infrastructure` y `presentation`. El dominio usa Java y define los puertos de persistencia, consulta de mascotas y publicación de eventos. Los adaptadores implementan esos puertos.
- Citas obtiene la ficha de Mascotas y Dueños por REST. Los tres eventos salen por RabbitMQ. El servicio de demo suministra fichas ficticias para probar el flujo local durante la integración con el equipo.

## Agenda

Por defecto todos los veterinarios usan una jornada diaria de 08:00 a 18:00 en `America/Bogota` y turnos de 30 minutos. Solo se reservan turnos futuros que empiezan en una división de la jornada y terminan dentro de ella. La disponibilidad excluye turnos vencidos y solapamientos con citas que no estén canceladas.

La duración queda guardada con cada cita. PostgreSQL rechaza intervalos solapados de un mismo veterinario o una misma mascota, incluso con solicitudes concurrentes o si cambia la duración configurada. Dos turnos pueden tocarse en sus extremos. Cancelar una cita libera el turno y repetir esa cancelación devuelve la misma cita cancelada. Una cita completada no se cancela.

Antes de escribir, el servicio revisa el cruce con la misma regla de intervalos que usa PostgreSQL y responde `409` indicando si el ocupado es el veterinario o la mascota. Las restricciones de la base siguen siendo la garantía cuando dos solicitudes llegan a la vez.

Los valores se configuran con `PETVET_AGENDA_ZONA`, `PETVET_AGENDA_APERTURA`, `PETVET_AGENDA_CIERRE` y `PETVET_AGENDA_DURACION_MINUTOS` en el proceso del servicio. Se adoptaron como valores locales de demostración y quedan por ratificar con el equipo.

## Transacciones y consulta a Mascotas

Agendar, registrar la consulta y generar recordatorios consultan la ficha en Mascotas y Dueños antes de abrir la transacción. Así, si ese servicio tarda o no responde, Citas devuelve `503` sin retener una conexión a PostgreSQL ni el bloqueo de la cita. La transacción solo cubre las escrituras: la cita o el historial, el cambio de estado y el evento en la outbox.

Al registrar la consulta, la cita se lee primero sin bloqueo para validar y, ya dentro de la transacción, se bloquea y se confirma que sigue agendada. Si dos registros de la misma consulta llegan a la vez, uno responde `201` y el otro `409`.

El historial carga sus listas de vacunas, tratamientos y exámenes por lotes (`hibernate.default_batch_fetch_size`). Leer el historial de una mascota usa un número fijo de consultas SQL, sin importar cuántos registros tenga.

## Vacunación de demostración

Las reglas son un calendario académico simplificado con nombres de vacunas reales; no reemplazan el criterio del veterinario. `SelectorEsquemaVacunacion` toma la especie y la edad en meses cumplidos para elegir un esquema. Perros y gatos usan estas reglas. Para `AVE` y `OTRO` se devuelve una lista vacía. La vacuna combinada es `Polivalente` en perros y `Triple felina` en gatos. Cada vacuna se calcula por separado desde su última aplicación registrada, y el resultado sale en orden de vencimiento.

| Edad | Vacuna | Primera fecha límite | Refuerzo |
| --- | --- | --- | --- |
| Menos de 12 meses | Combinada | Nacimiento + 2 meses | Se omite si ya figura aplicada. |
| Menos de 12 meses | `Rabia` | Nacimiento + 3 meses | Se omite si ya figura aplicada. |
| 12 a 83 meses | Combinada y `Rabia` | Nacimiento + 12 meses | Última aplicación + 12 meses. |
| 84 meses en adelante | `Rabia` | Nacimiento + 84 meses | Última aplicación + 12 meses. |
| 84 meses en adelante | Combinada | Nacimiento + 84 meses | Última aplicación + 36 meses. |

`RecordatoriosVacunasJob` revisa las mascotas conocidas por Citas, incluidas las que tienen o tuvieron una cita y aún no tienen historial clínico. La tarea corre a las 08:00 en la zona configurada y cada minuto en el Compose de demo. Publica si el vencimiento cae entre hoy y hoy más siete días, incluidos los extremos. La clave mascota, vacuna y fecha límite evita generar el mismo recordatorio al volver a ejecutar la tarea. El cálculo puede mostrar una vacuna vencida en la consulta de próximas vacunas. El recordatorio aplica su propia ventana.

Si Mascotas y Dueños no responde, la tarea corta esa ejecución y retoma en la siguiente, en lugar de esperar el tiempo límite por cada mascota. Un error con una mascota puntual, por ejemplo una ficha eliminada, no impide revisar las demás. La tarea y el envío de la outbox usan hilos de planificación separados (`spring.task.scheduling.pool.size`), para que una no retrase a la otra.

## Publicación durable

`OutboxPublicadorEventos` guarda el evento en PostgreSQL dentro de la misma transacción de la cita o la consulta. `OutboxDispatcher` lo envía a RabbitMQ y marca el registro como enviado al recibir confirmación sin devolución. Si el broker está caído o no hay cola enlazada, el registro queda pendiente y se reintenta. El reintento reutiliza el `messageId` del evento. Cada ejecución envía hasta 50 eventos pendientes. Si el broker no responde, se detiene en el primero y espera a la siguiente ejecución. Si solo falta la cola de un evento, continúa con los demás.

La entrega es al menos una vez. Si RabbitMQ acepta un mensaje y el proceso se detiene antes de marcarlo como enviado en PostgreSQL, puede aparecer una copia con el mismo identificador. Pia debe usar ese identificador para evitar procesar dos veces la misma notificación. [Contrato de eventos](api.md#eventos-rabbitmq).

## Pendientes de integración

- Confirmar con Pia la ficha de mascota y probar la consulta REST con su servicio. Si se necesita revisar también a las mascotas que nunca han tenido una cita, acordar cómo consultar el padrón del servicio de Pia. Confirmar que Notificaciones enlaza las colas, procesa los tres eventos y evita duplicados antes de enviar el correo de pruebas.
- Ratificar con Camilo, Andrés Daza y Pia los formatos de entrada, salida y errores de Citas. Confirmar horarios y reglas de la demo para la sustentación.
- Conectar el frontend y verificar el recorrido en equipo, incluido el correo en MailHog. El flujo local de Citas prueba hasta las colas de demostración.
