# ADR-007: Choques de horario validados por PostgreSQL

- **Estado:** Aceptada
- **Fecha:** 12 de noviembre de 2026

## Contexto

Un veterinario no puede tener dos citas que se crucen, y una mascota tampoco. Si dos personas reservan el mismo turno al mismo tiempo, una validación solo en el código ("¿ya hay una cita a esa hora?") puede dejar pasar las dos, porque ambas consultan antes de que la otra guarde.

## Decisión

La regla vive en la base: restricciones de exclusión de PostgreSQL (`EXCLUDE USING gist`, con la extensión `btree_gist`) sobre el intervalo de cada cita, una por veterinario y otra por mascota, sin contar las canceladas. Antes de escribir, el servicio revisa el cruce con la misma regla para responder `409` diciendo si el ocupado es el veterinario o la mascota, pero la garantía es la base. Cada cita guarda su duración (30 minutos por defecto) y debe caer dentro del horario del veterinario.

## Opciones evaluadas

- **Validar solo en el código:** portable entre bases, pero falla con reservas simultáneas.
- **Bloquear la agenda del veterinario al reservar:** evita el choque, pero serializa todas las reservas de ese veterinario y es más fácil equivocarse.
- **Índice único por veterinario y hora de inicio:** sencillo, pero no detecta citas que se cruzan sin empezar a la misma hora.

## Consecuencias

- **A favor:** correcto aunque lleguen reservas al mismo tiempo; lo prueba `CitasApiIT` con solicitudes simultáneas. El usuario ve un mensaje claro (pruebas P6 y P7).
- **En contra:** depende de una característica propia de PostgreSQL; cambiar de motor obligaría a reescribir la regla. La regla queda repartida entre la migración SQL y el servicio.
