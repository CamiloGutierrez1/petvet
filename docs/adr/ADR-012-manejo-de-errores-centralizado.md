# ADR-012: Manejo de errores centralizado con un formato único

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

El frontend tenía que mostrar al usuario por qué falló una operación: turno ocupado, correo repetido, mascota que no existe, servicio caído. Si cada controlador devolviera los errores a su manera, el front necesitaría un caso distinto por pantalla, y las excepciones de Spring o de la base podrían llegar al usuario con mensajes técnicos en inglés.

## Decisión

El dominio lanza excepciones propias (`RecursoNoEncontradoException`, `ConflictoException`, `DependenciaNoDisponibleException`) y cada servicio tiene un único `ApiExceptionHandler` con `@RestControllerAdvice` que las traduce a un código HTTP y a un cuerpo `{error, detalle}`: `error` es un código estable (`CONFLICTO`, `RECURSO_NO_ENCONTRADO`, `DEPENDENCIA_NO_DISPONIBLE`, `VALIDACION`) y `detalle` es el mensaje en español para el usuario. Los errores de la base, como la violación de la restricción de choque de horario, también se traducen a `409` con un mensaje claro.

## Opciones evaluadas

- **`try/catch` en cada controlador:** el manejo queda repetido y cada endpoint termina respondiendo distinto.
- **Tipos `Result` en lugar de excepciones:** explícito, pero poco común en Spring y obliga a desenvolver el resultado en cada capa.
- **El formato por defecto de Spring Boot (`ProblemDetail` o `/error`):** estándar, pero con mensajes técnicos que el front tendría que traducir uno por uno.

## Consecuencias

- **A favor:** el front tiene una sola función que lee `detalle` y lo muestra donde ocurrió el error (prueba P3, P6, P7). Los casos de uso no saben nada de HTTP. Un error inesperado responde `500` con un mensaje genérico y el detalle queda en el log, no en la pantalla.
- **En contra:** hay que mantener las tres copias del manejador alineadas con el mismo contrato, y cada excepción nueva del dominio necesita su entrada en el manejador.
