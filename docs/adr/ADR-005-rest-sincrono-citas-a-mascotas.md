# ADR-005: REST síncrono de Citas a Mascotas y Dueños

- **Estado:** Aceptada
- **Fecha:** 11 de noviembre de 2026

## Contexto

Para agendar, registrar una consulta o calcular vacunas, Citas necesita saber que la mascota existe, su especie, su fecha de nacimiento y el correo del dueño. Esos datos son de Mascotas y Dueños ([ADR-002](ADR-002-base-de-datos-por-servicio.md)).

## Decisión

Citas consulta `GET /api/mascotas/{id}` de Mascotas y Dueños por REST cada vez que necesita la ficha, detrás del puerto `MascotaClient`. La llamada tiene límites de 2 segundos para conectar y 3 para leer, y se hace antes de abrir la transacción. Si Mascotas no responde, Citas devuelve `503` con un mensaje claro.

## Opciones evaluadas

- **Copia local de las mascotas en Citas, actualizada por eventos:** Citas podría agendar aunque Mascotas esté caído, pero hay que mantener la copia sincronizada y puede quedar desactualizada. Es más trabajo del que justifica el proyecto.
- **Que el front envíe los datos de la mascota al agendar:** evita la llamada, pero Citas confiaría en datos que no puede verificar.

## Consecuencias

- **A favor:** simple de entender y de probar; Citas siempre usa los datos actuales de la mascota. Como la llamada se hace fuera de la transacción, una demora de Mascotas no retiene conexiones ni bloqueos de la base de Citas.
- **En contra:** acopla la disponibilidad: sin Mascotas no se puede agendar ni registrar una consulta (prueba P12). Es el punto donde el sistema elige consistencia sobre disponibilidad, a diferencia de las notificaciones ([ADR-003](ADR-003-rabbitmq-para-notificaciones.md)).
