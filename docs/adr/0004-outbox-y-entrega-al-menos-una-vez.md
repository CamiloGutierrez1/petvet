# ADR 0004: Outbox y entrega al menos una vez con deduplicación

- **Estado:** Aceptada
- **Fecha:** 11 de noviembre de 2026

## Contexto

Guardar la cita en PostgreSQL y publicar el evento en RabbitMQ son dos operaciones en sistemas distintos. Si se guarda la cita y el broker está caído, el evento se pierde y el dueño nunca recibe el correo. Si se publica primero y la transacción falla, llega un correo de una cita que no existe.

## Decisión

- **En Citas, patrón outbox:** `OutboxPublicadorEventos` guarda el evento en una tabla de la misma base y en la misma transacción que la cita o la consulta. `OutboxDispatcher` revisa la tabla cada 5 segundos, envía los pendientes a RabbitMQ y los marca como enviados solo cuando el broker confirma. Cada evento tiene un `messageId` que no cambia entre reintentos.
- **En Notificaciones, deduplicación:** `EnvioSinDuplicados` registra el `messageId` en una tabla con restricción única, en la misma transacción del envío. Si ya existe, no envía. Las fallas pasajeras se reintentan cinco veces con espera creciente, y después el mensaje pasa a una DLQ desde donde se puede reprocesar.

## Alternativas consideradas

- **Publicar directo desde el caso de uso:** simple, pero se pierden eventos con el broker caído.
- **Transacciones distribuidas (2PC):** garantizan las dos escrituras juntas, pero RabbitMQ no las soporta de forma práctica y agregan mucha complejidad.

## Consecuencias

- **A favor:** ningún evento se pierde aunque RabbitMQ o Notificaciones estén caídos; se entregan cuando vuelven. Un mensaje repetido no genera un segundo correo.
- **En contra:** la entrega es "al menos una vez", no "exactamente una vez": el consumidor debe ser idempotente. Hay una tabla y un proceso periódico más, y el correo puede tardar hasta el siguiente ciclo del despachador.
