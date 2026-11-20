# Registro de decisiones de arquitectura (ADR)

Cada ADR documenta una decisión importante: el contexto, lo que se decidió, las opciones evaluadas y sus consecuencias, es decir, el trade-off. El resumen de los trade-offs y los diagramas C4 están en [Arquitectura](../arquitectura.md).

| ADR | Decisión | Atributo que favorece | Fecha | Estado |
| --- | --- | --- | --- | --- |
| [ADR-001](ADR-001-microservicios.md) | Microservicios en lugar de un monolito | Mantenibilidad, disponibilidad | 10 nov | Aceptada |
| [ADR-002](ADR-002-base-de-datos-por-servicio.md) | Una base PostgreSQL por servicio | Independencia de los servicios | 10 nov | Aceptada |
| [ADR-003](ADR-003-rabbitmq-para-notificaciones.md) | Notificaciones por eventos en RabbitMQ | Disponibilidad | 10 nov | Aceptada |
| [ADR-004](ADR-004-outbox-y-entrega-al-menos-una-vez.md) | Outbox y entrega al menos una vez con deduplicación | Confiabilidad | 11 nov | Aceptada |
| [ADR-005](ADR-005-rest-sincrono-citas-a-mascotas.md) | REST síncrono de Citas a Mascotas y Dueños | Simplicidad, datos actualizados | 11 nov | Aceptada |
| [ADR-006](ADR-006-clean-architecture-por-servicio.md) | Clean Architecture dentro de cada servicio | Mantenibilidad, facilidad de prueba | 10 nov | Aceptada |
| [ADR-007](ADR-007-choques-de-horario-en-postgresql.md) | Choques de horario validados por PostgreSQL | Consistencia | 12 nov | Aceptada |
| [ADR-008](ADR-008-spa-react-con-capa-api.md) | SPA en React con capa de API y mocks | Mantenibilidad, desarrollo en paralelo | 10 nov | Aceptada |
| [ADR-009](ADR-009-perfil-simulado-sin-login.md) | Perfil simulado sin inicio de sesión | Alcance del proyecto | 10 nov | Aceptada |
| [ADR-010](ADR-010-gitflow-y-conventional-commits.md) | Gitflow con ramas protegidas y Conventional Commits | Mantenibilidad, trabajo en equipo | 10 nov | Aceptada |
| [ADR-011](ADR-011-migraciones-con-flyway.md) | Migraciones de base de datos con Flyway | Mantenibilidad, portabilidad | 10 nov | Aceptada |
| [ADR-012](ADR-012-manejo-de-errores-centralizado.md) | Manejo de errores centralizado con un formato único | Usabilidad, mantenibilidad | 10 nov | Aceptada |
| [ADR-013](ADR-013-docker-compose-para-desarrollo-local.md) | Docker Compose para levantar el sistema en local | Portabilidad | 10 nov | Aceptada |
| [ADR-014](ADR-014-estrategia-de-pruebas.md) | Pruebas unitarias, de integración con Testcontainers y manuales | Confiabilidad | 11 nov | Aceptada |

Para una decisión nueva, copia la estructura de cualquier ADR con el siguiente número (`ADR-015-…md`). Si una decisión cambia, no se borra su ADR: se marca como "Reemplazada por ADR-NNN" y se enlaza la nueva.
