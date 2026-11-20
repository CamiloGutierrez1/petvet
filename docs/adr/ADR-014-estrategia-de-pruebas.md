# ADR-014: Pruebas unitarias, de integración con Testcontainers y manuales de punta a punta

- **Estado:** Aceptada
- **Fecha:** 11 de noviembre de 2026

## Contexto

Las reglas más delicadas de PetVet dependen de la infraestructura real: los choques de horario los garantiza una restricción de PostgreSQL ([ADR-007](ADR-007-choques-de-horario-en-postgresql.md)), la entrega de eventos depende de RabbitMQ y de la outbox ([ADR-004](ADR-004-outbox-y-entrega-al-menos-una-vez.md)) y la deduplicación depende de la base de Notificaciones. Probar todo con objetos simulados dejaría sin probar justamente eso. Al mismo tiempo, la mayoría de las reglas del dominio son Java puro y se pueden probar rápido.

## Decisión

Usamos tres niveles. **Unitarias** con JUnit 5 para el dominio y los casos de uso (`VacunacionStrategyTest`, `AgendarCitaUseCaseTest`, `OutboxDispatcherTest`, …), que corren con `mvn test`. **De integración** (`*IT`, con el plugin failsafe) que levantan PostgreSQL y RabbitMQ reales con Testcontainers: `CitasApiIT` prueba reservas simultáneas, `OutboxRabbitIT` la publicación y `NotificacionesRabbitIT` que un evento repetido no envía dos correos. **Manuales de punta a punta**, del front contra los servicios reales en Docker Compose, con el plan P1 a P12 de [Demo y plan de pruebas](../demo-y-pruebas.md), más los scripts `scripts/probar_citas.py` y `scripts/verificar_local.py`.

## Opciones evaluadas

- **Solo pruebas unitarias con mocks:** rápidas, pero no detectan errores de SQL, de la restricción de exclusión ni de la configuración de RabbitMQ.
- **Base H2 en memoria para la integración:** sin Docker, pero H2 no soporta `EXCLUDE USING gist` ni `tsrange`; la prueba pasaría en H2 y fallaría en PostgreSQL.
- **Pruebas de punta a punta automáticas (Playwright o Cypress):** ideales a largo plazo, pero escribirlas y mantenerlas no cabía en el tiempo del proyecto.

## Consecuencias

- **A favor:** las reglas críticas se prueban contra la tecnología real. El plan manual P1 a P12 pasó completo el 14 de noviembre y detectó un error de integración que se corrigió en el pull request 32.
- **En contra:** las pruebas de integración necesitan Docker y tardan más. Las pruebas de punta a punta son manuales, así que hay que repetirlas a mano después de cada cambio importante.
