# ADR-011: Migraciones de base de datos con Flyway

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

Cada servicio tiene su propia base PostgreSQL ([ADR-002](ADR-002-base-de-datos-por-servicio.md)) y el esquema iba a cambiar durante el proyecto: duración de las citas, restricciones de choque de horario, cirugías, horario propio de cada veterinario, peso con tres decimales. Cualquier integrante debía poder levantar las bases desde cero con el mismo esquema, y la base de un compañero no podía quedar desactualizada.

## Decisión

Cada servicio versiona su esquema con Flyway en `src/main/resources/db/migration` (`V1__…`, `V2__…`). Las migraciones se aplican solas al arrancar el servicio. Hibernate solo valida que las entidades coincidan con las tablas (`ddl-auto: validate`) y nunca crea ni altera tablas. Los datos de ejemplo para la demo van aparte, en `db/demo`, y solo se cargan si se agrega esa ruta a `SPRING_FLYWAY_LOCATIONS`.

## Opciones evaluadas

- **`ddl-auto: update` de Hibernate:** cero archivos SQL, pero no deja historial, no sabe borrar ni renombrar columnas y no puede crear la restricción `EXCLUDE USING gist` que necesita [ADR-007](ADR-007-choques-de-horario-en-postgresql.md).
- **Liquibase:** hace lo mismo con más formatos (XML, YAML), pero agrega complejidad que no necesitábamos; con SQL plano el equipo ya sabía escribir las migraciones.
- **Scripts SQL manuales:** cada persona tendría que acordarse de correrlos en orden; fácil terminar con bases distintas.

## Consecuencias

- **A favor:** el esquema está en Git junto al código que lo usa. Citas llegó a `V7` y Mascotas a `V3` sin que nadie tuviera que tocar su base a mano. Si una entidad no coincide con la tabla, el servicio no arranca, y el error se ve en el momento y no en producción.
- **En contra:** una migración ya aplicada no se puede editar; cada corrección es una migración nueva (por ejemplo `V4__indices_unicos_redundantes.sql`). Las bases de desarrollo viejas a veces hubo que borrarlas con `docker compose down -v`.
