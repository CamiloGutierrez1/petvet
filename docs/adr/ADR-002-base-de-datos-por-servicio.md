# ADR-002: Una base PostgreSQL por servicio

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

Con microservicios ([ADR-001](ADR-001-microservicios.md)), compartir una base haría que un cambio de tabla en un servicio rompa a los otros, y los acoplaría por debajo aunque sus APIs estén separadas.

## Decisión

Cada servicio tiene su propia base PostgreSQL 16 en su propio contenedor (`postgres-mascotas`, `postgres-citas` y `postgres-notificaciones`), y Flyway versiona su esquema. Ningún servicio lee la base de otro: si Citas necesita los datos de una mascota, los pide por REST ([ADR-005](ADR-005-rest-sincrono-citas-a-mascotas.md)).

## Opciones evaluadas

- **Una base compartida con esquemas separados:** menos contenedores, pero cualquier servicio podría leer las tablas de otro y el aislamiento quedaría solo en la disciplina del equipo.
- **Una base compartida sin separación:** permite consultas con `JOIN` entre mascotas y citas, pero es un monolito de datos.

## Consecuencias

- **A favor:** cada servicio es dueño de sus datos y puede cambiar su esquema sin coordinar con los demás. Una falla o un cambio en una base no afecta a las otras.
- **En contra:** no hay transacciones ni consultas entre bases. Por ejemplo, la lista de pacientes del veterinario sale de Citas y los nombres de las mascotas se piden aparte a Mascotas; si ese servicio no responde, la lista muestra "Mascota #N" (prueba P12). Citas guarda solo el `mascotaId`, no una copia de la mascota.
