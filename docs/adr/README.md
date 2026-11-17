# Registro de decisiones de arquitectura (ADR)

Cada ADR documenta una decisión importante: el contexto, lo que se decidió, las alternativas que se descartaron y sus consecuencias, es decir, el trade-off. El resumen de los trade-offs y los diagramas C4 están en [Arquitectura](../arquitectura.md).

| ADR | Decisión | Atributo que favorece | Estado |
| --- | --- | --- | --- |
| [0001](0001-microservicios.md) | Microservicios en lugar de un monolito | Mantenibilidad, disponibilidad | Aceptada |
| [0002](0002-base-de-datos-por-servicio.md) | Una base PostgreSQL por servicio | Independencia de los servicios | Aceptada |
| [0003](0003-rabbitmq-para-notificaciones.md) | Notificaciones por eventos en RabbitMQ | Disponibilidad | Aceptada |
| [0004](0004-outbox-y-entrega-al-menos-una-vez.md) | Outbox y entrega al menos una vez con deduplicación | Confiabilidad | Aceptada |
| [0005](0005-rest-sincrono-citas-a-mascotas.md) | REST síncrono de Citas a Mascotas y Dueños | Simplicidad, datos actualizados | Aceptada |
| [0006](0006-clean-architecture-por-servicio.md) | Clean Architecture dentro de cada servicio | Mantenibilidad, facilidad de prueba | Aceptada |
| [0007](0007-choques-de-horario-en-postgresql.md) | Choques de horario validados por PostgreSQL | Consistencia | Aceptada |
| [0008](0008-spa-react-con-capa-api.md) | SPA en React con capa de API y mocks | Mantenibilidad, desarrollo en paralelo | Aceptada |
| [0009](0009-perfil-simulado-sin-login.md) | Perfil simulado sin inicio de sesión | Alcance del proyecto | Aceptada |

Para una decisión nueva, copia la estructura de cualquier ADR con el siguiente número. Si una decisión cambia, no se borra su ADR: se marca como reemplazada y se enlaza la nueva.
