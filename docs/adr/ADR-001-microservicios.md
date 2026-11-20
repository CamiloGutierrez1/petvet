# ADR-001: Microservicios en lugar de un monolito

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

PetVet tiene tres partes del dominio con responsabilidades distintas: el registro de dueños y mascotas, la agenda con el historial clínico, y el envío de correos. El catálogo del proyecto pide tres servicios, y el equipo de cuatro personas necesitaba trabajar en paralelo sin pisarse. Enviar correos no debe frenar el agendamiento.

## Decisión

Separar el sistema en tres microservicios Spring Boot, cada uno con su propia base de datos y su propio puerto:

- `ms-mascotas-duenos` (8081): dueños, mascotas, fotos y la ficha que consumen los demás.
- `ms-citas-historial` (8082): veterinarios, agenda, consultas, historial y esquemas de vacunación.
- `ms-notificaciones` (8083): consume eventos y envía los correos.

Se levantan juntos con Docker Compose y comparten un `pom.xml` raíz solo para las dependencias comunes.

## Opciones evaluadas

- **Monolito en capas:** más simple de levantar y de probar, con una sola base y transacciones locales. Se descartó porque una falla del correo o de un módulo afectaría a todo el sistema y porque el catálogo pide servicios independientes.
- **Monolito modular:** módulos separados dentro de un solo proceso. Mejora la organización, pero no permite mostrar el fallo aislado de una parte ni la comunicación por eventos.

## Consecuencias

- **A favor:** cada servicio cambia, se prueba y falla por separado. Si Notificaciones se cae, se sigue agendando (prueba P11). Cada parte se construyó con contratos claros (`docs/api.md`).
- **En contra:** más infraestructura (tres bases, un broker, un servidor de correo), llamadas por red que pueden fallar y pruebas de punta a punta más difíciles. Por eso existen `scripts/probar_citas.py` y el plan de pruebas manuales.
