# ADR-003: Notificaciones por eventos en RabbitMQ

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

Al agendar una cita, registrar una consulta o acercarse una vacuna, el dueño debe recibir un correo. El catálogo pide que agendar funcione aunque Notificaciones esté inactivo. Si Citas llamara a Notificaciones directamente, una caída del correo haría fallar el agendamiento.

## Decisión

Citas publica tres eventos en el exchange `petvet.events` de RabbitMQ: `cita.agendada`, `cita.completada` y `vacuna.proxima`. Notificaciones los consume desde sus colas (`notificaciones.citas` y `notificaciones.vacunas`) y envía un correo por evento. Citas no sabe quién consume sus eventos.

## Opciones evaluadas

- **Llamada REST de Citas a Notificaciones:** más simple, pero acopla la disponibilidad de los dos servicios y Citas tendría que reintentar por su cuenta.
- **Enviar el correo desde Citas:** un servicio menos, pero mezcla responsabilidades (SRP) y una falla del SMTP afectaría la agenda.
- **Kafka:** pensado para grandes volúmenes y para releer el historial de eventos; demasiado para tres tipos de eventos en una demo local.

## Consecuencias

- **A favor:** agendar y registrar la consulta no dependen del correo. Con Notificaciones apagado, la cita se guarda y el correo llega al volver (prueba P11). Agregar otro consumidor, por ejemplo un servicio de reportes, no cambia Citas.
- **En contra:** el correo llega unos segundos después de la operación (consistencia eventual), hay un componente más que operar, y un mensaje puede llegar dos veces, lo que obliga a deduplicar ([ADR-004](ADR-004-outbox-y-entrega-al-menos-una-vez.md)).
