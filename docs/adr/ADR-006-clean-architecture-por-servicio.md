# ADR 0006: Clean Architecture dentro de cada servicio

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

El profesor evalúa patrones, SOLID y mantenibilidad. Las reglas del negocio (choques de horario, esquemas de vacunación, validación del historial) no deberían depender de Spring, de JPA, de RabbitMQ ni del SMTP, para poder probarlas y cambiar la tecnología sin tocarlas.

## Decisión

Los tres servicios usan las mismas cuatro capas, con dependencias hacia adentro:

- `presentation`: controladores REST que validan y traducen HTTP.
- `application`: casos de uso, por ejemplo `AgendarCitaUseCase` o `RegistrarConsultaUseCase`.
- `domain`: entidades, reglas y puertos (interfaces), en Java sin Spring.
- `infrastructure`: adaptadores que implementan los puertos (JPA, cliente REST, outbox, SMTP).

Los casos de uso reciben los puertos por constructor (DIP). En este marco viven los patrones del proyecto: Builder (`RegistroHistorialBuilder`), Strategy (`VacunacionStrategy`) y Adapter (`CorreoSmtpAdapter`).

## Alternativas consideradas

- **Capas clásicas controlador-servicio-repositorio:** menos clases, pero el servicio depende directamente de JPA y las reglas quedan mezcladas con la persistencia.
- **Arquitectura distinta por servicio:** más libertad, pero se pierde la coherencia y cuesta más explicar y revisar.

## Consecuencias

- **A favor:** el dominio se prueba sin base de datos ni Spring (pruebas unitarias rápidas). Cambiar la forma de guardar el historial o el proveedor de correo solo toca un adaptador. Los tres servicios se leen igual.
- **En contra:** más clases e interfaces, incluso para operaciones sencillas, y una curva de aprendizaje para el equipo.
