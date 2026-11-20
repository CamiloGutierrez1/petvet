# Requerimientos de PetVet

PetVet es el sistema de gestión de una clínica veterinaria (Proyecto 5 del catálogo). Este documento reúne los requerimientos funcionales y no funcionales, dónde se implementa cada uno y con qué prueba se verifica. Las pruebas `P1` a `P12` están en [Demo y plan de pruebas](demo-y-pruebas.md).

## Actores

| Actor | Qué hace en el sistema |
| --- | --- |
| Dueño | Registra sus datos y sus mascotas, agenda y cancela citas, consulta la ficha y el historial de sus mascotas. |
| Veterinario | Consulta su agenda y sus pacientes, registra la consulta y revisa el historial clínico. |
| Servidor de correo | Sistema externo que entrega los correos al dueño. En local es MailHog. |

## Requerimientos funcionales

Son 10, como pide la guía. Todos están implementados y probados; lo que quedó fuera del alcance está al final de la tabla.

| Id | Requerimiento | Servicio | Estado | Prueba |
| --- | --- | --- | --- | --- |
| RF1 | Registrar un dueño con nombre, correo, documento, teléfono y dirección. | Mascotas y Dueños | Implementado | P2, P3 |
| RF2 | Registrar y editar las mascotas de un dueño (nombre, especie, raza, fecha de nacimiento, peso y foto) y consultar su ficha con los datos del dueño. | Mascotas y Dueños | Implementado | P1, P9 |
| RF3 | Listar los veterinarios con su especialidad y horario, y consultar sus turnos libres en una fecha. | Citas e Historial | Implementado | P4 |
| RF4 | Agendar una cita para una mascota con un veterinario, una fecha y hora y un tipo de servicio (consulta general, vacunación, control o especialista). | Citas e Historial | Implementado | P5, P6, P7 |
| RF5 | Cancelar una cita agendada y liberar su turno. | Citas e Historial | Implementado | P10 |
| RF6 | Consultar la agenda de un veterinario por día y la lista de sus pacientes. | Citas e Historial | Implementado | P12 |
| RF7 | Registrar la consulta de una cita: diagnóstico, vacunas aplicadas, tratamientos, exámenes, cirugías, observaciones y próximo control. | Citas e Historial | Implementado | P8 |
| RF8 | Consultar el historial clínico de una mascota y sus próximas vacunas según especie y edad. | Citas e Historial | Implementado | P1, P8 |
| RF9 | Enviar al dueño un correo al agendar (con los datos del veterinario) y otro con el resumen de la consulta, los exámenes y las próximas vacunas o controles. | Notificaciones | Implementado | P5, P8 |
| RF10 | Revisar cada día las vacunas que vencen en los próximos siete días y avisar al dueño por correo. | Citas e Historial y Notificaciones | Implementado | `scripts/probar_citas.py --esperar-vacuna` |
| — | Ficha de la mascota en PDF, calendario visual de citas y reportes (bonificaciones del catálogo). | — | Fuera del alcance | — |
| — | Inicio de sesión con usuario y contraseña ([ADR-009](adr/ADR-009-perfil-simulado-sin-login.md)). | — | Fuera del alcance | — |

> **CAMBIO.** La primera versión tenía 15 requerimientos. Se agruparon en 10 para cumplir el máximo de la guía, sin quitar funcionalidad: registrar y consultar mascotas quedaron juntos (RF2), igual que veterinarios y turnos (RF3), historial y vacunas (RF8) y los dos correos (RF9). Las condiciones que antes eran requerimientos, como "impedir dos citas en el mismo horario", pasaron a la tabla de reglas de negocio.

Sobre RF9: Citas guarda el nombre de cada examen, no su resultado. El aviso de "exámenes" del catálogo se cubre en el correo del resumen, que lista los exámenes hechos en la consulta.

## Reglas de negocio

Cada regla está en el código del dominio o en la base de datos, y el usuario recibe el mensaje en español que aparece en la tabla.

| Id | Regla | Dónde se aplica | Si no se cumple |
| --- | --- | --- | --- |
| RN1 | El correo del dueño es único y debe ser válido. | Mascotas y Dueños (dominio y restricción única en la base) | `409` "Ya existe un dueño registrado con ese correo." |
| RN2 | La fecha de nacimiento de una mascota no puede ser futura ni anterior a 1900, y el peso debe ser mayor que 0 (hasta tres decimales). | Mascotas y Dueños | `400` con el mensaje del campo |
| RN3 | La foto de una mascota debe ser JPG, PNG o WebP y pesar máximo 5 MB. | Mascotas y Dueños | `400` "La foto pesa más de 5 MB." |
| RN4 | Una cita debe ser en el futuro, empezar en un turno exacto (cada 30 minutos) y quedar dentro del horario de su veterinario. | Citas e Historial (`AgendaService`) | `400` "La cita debe quedar dentro del horario del veterinario, de HH:mm a HH:mm" |
| RN5 | Un veterinario no puede tener dos citas que se crucen. | Citas e Historial: validación previa y restricción `EXCLUDE` en PostgreSQL ([ADR-007](adr/ADR-007-choques-de-horario-en-postgresql.md)) | `409` "El veterinario ya tiene una cita en ese horario" |
| RN6 | Una mascota no puede tener dos citas que se crucen, aunque sean con veterinarios distintos. | Igual que RN5 | `409` "La mascota ya tiene una cita en ese horario" |
| RN7 | Las citas canceladas no ocupan turno. | Restricción `EXCLUDE … WHERE estado <> 'CANCELADA'` | El turno vuelve a aparecer libre |
| RN8 | Solo se registra la consulta de una cita agendada, y una cita completada no se puede cancelar. | Citas e Historial (`RegistrarConsultaUseCase`, `AgendaService`) | `409` "Solo puede registrarse la consulta de una cita agendada" |
| RN9 | La consulta exige diagnóstico, y el próximo control debe ser igual o posterior a la fecha de la consulta. | Citas e Historial (`RegistroHistorial`, armado con su Builder) | `400` "El diagnóstico es obligatorio" |
| RN10 | Las próximas vacunas se calculan con el esquema de la especie y la edad de la mascota; se avisa siete días antes del vencimiento. | Citas e Historial (`VacunacionStrategy`, tarea diaria) | — |
| RN11 | El dueño recibe un solo correo por evento, aunque RabbitMQ entregue el mensaje dos veces. | Notificaciones (deduplicación por `messageId`, [ADR-004](adr/ADR-004-outbox-y-entrega-al-menos-una-vez.md)) | El mensaje repetido se descarta |

## Requerimientos no funcionales

Cada atributo de calidad se escribe como un escenario: qué pasa, cómo responde el sistema y cómo se mide. La explicación de cómo lo logra la arquitectura está en [Arquitectura](arquitectura.md#atributos-de-calidad).

| Id | Atributo | Escenario | Medida | Prueba |
| --- | --- | --- | --- | --- |
| RNF1 | Disponibilidad | Notificaciones o RabbitMQ están caídos y un dueño agenda una cita. | La cita se guarda con `201`. El correo llega cuando el servicio vuelve, sin pasos manuales. | P11 |
| RNF2 | Disponibilidad | Mascotas y Dueños está caído y el veterinario abre sus pacientes o un dueño intenta agendar. | La lista de pacientes carga con "Mascota #N". Agendar responde `503` con un mensaje claro y nada queda a medias. Al volver el servicio, todo funciona sin reiniciar nada más. | P12 |
| RNF3 | Consistencia | Dos personas reservan el mismo turno a la vez. | Solo una reserva se guarda; la otra recibe `409`. Lo garantiza PostgreSQL, no el código. | P6, `CitasApiIT` |
| RNF4 | Confiabilidad | RabbitMQ entrega dos veces el mismo evento. | El dueño recibe un solo correo. | `NotificacionesRabbitIT` |
| RNF5 | Mantenibilidad | Se cambia la forma de guardar el historial, por ejemplo otra base de datos. | Solo cambia el adaptador de `infrastructure`; el dominio, los casos de uso y el agendamiento no se tocan. | Revisión del código por capas |
| RNF6 | Extensibilidad | Se agrega un esquema de vacunación para una especie nueva. | Se crea una clase de `VacunacionStrategy` y se registra; el selector y los esquemas existentes no cambian. | `VacunacionStrategyTest` |
| RNF7 | Usabilidad | El back rechaza una operación (turno ocupado, correo repetido, datos inválidos). | El front muestra el mensaje en español que envía el back en `detalle`, en el lugar del error. | P3, P6, P7 |
| RNF8 | Rendimiento del front | Un usuario abre PetVet por primera vez. | La primera carga baja de 886 KB a 383 KB porque cada pantalla se descarga al visitarla. | `npm run build` |
| RNF9 | Portabilidad | Un integrante levanta el sistema en su equipo. | Un solo comando de Docker Compose levanta los servicios, las bases, RabbitMQ, MailHog y el front. | [README](../README.md#arranque-local) |

## Restricciones

- El sistema corre en local con Docker Compose; no se despliega en la nube.
- Backend en Java 21 y Spring Boot 3.5; frontend en React con TypeScript.
- Sin inicio de sesión real: el dueño entra con su correo y el veterinario se elige de una lista. Es una decisión de alcance ([ADR-009](adr/ADR-009-perfil-simulado-sin-login.md)).
- Las reglas de vacunación son un calendario académico simplificado; no reemplazan el criterio del veterinario.
- Fuera de alcance (bonificaciones del catálogo): ficha en PDF, calendario visual y reportes.
