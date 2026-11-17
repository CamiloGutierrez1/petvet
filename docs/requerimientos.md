# Requerimientos de PetVet

PetVet es el sistema de gestión de una clínica veterinaria (Proyecto 5 del catálogo). Este documento reúne los requerimientos funcionales y no funcionales, dónde se implementa cada uno y con qué prueba se verifica. Las pruebas `P1` a `P12` están en [Demo y plan de pruebas](demo-y-pruebas.md).

## Actores

| Actor | Qué hace en el sistema |
| --- | --- |
| Dueño | Registra sus datos y sus mascotas, agenda y cancela citas, consulta la ficha y el historial de sus mascotas. |
| Veterinario | Consulta su agenda y sus pacientes, registra la consulta y revisa el historial clínico. |
| Servidor de correo | Sistema externo que entrega los correos al dueño. En local es MailHog. |

## Requerimientos funcionales

| Id | Requerimiento | Servicio | Prueba |
| --- | --- | --- | --- |
| RF1 | Registrar un dueño con nombre, correo, documento, teléfono y dirección. El correo es único. | Mascotas y Dueños | P2, P3 |
| RF2 | Registrar y editar mascotas de un dueño con nombre, especie (perro, gato, ave u otro), raza, fecha de nacimiento, peso y foto. Un dueño puede tener varias mascotas. | Mascotas y Dueños | P1 |
| RF3 | Consultar la ficha de una mascota con los datos de su dueño. | Mascotas y Dueños | P1, P9 |
| RF4 | Listar los veterinarios con su especialidad y su horario de atención. | Citas e Historial | P4 |
| RF5 | Consultar los turnos libres de un veterinario en una fecha, dentro de su horario. | Citas e Historial | P4 |
| RF6 | Agendar una cita para una mascota con un veterinario, una fecha y hora y un tipo de servicio (consulta general, vacunación, control o especialista). | Citas e Historial | P5 |
| RF7 | Impedir que un veterinario o una mascota tengan dos citas en el mismo horario. | Citas e Historial | P6, P7 |
| RF8 | Cancelar una cita agendada y liberar su turno. | Citas e Historial | P10 |
| RF9 | Consultar la agenda de un veterinario por día y la lista de sus pacientes. | Citas e Historial | P12 |
| RF10 | Registrar la consulta de una cita: diagnóstico, vacunas aplicadas, tratamientos, exámenes, cirugías, observaciones y próximo control. La cita pasa a completada. | Citas e Historial | P8 |
| RF11 | Consultar el historial clínico de una mascota. | Citas e Historial | P1, P8 |
| RF12 | Calcular las próximas vacunas según la especie y la edad de la mascota. | Citas e Historial | P1 |
| RF13 | Enviar al dueño un correo de confirmación al agendar una cita, con los datos del veterinario. | Notificaciones | P5 |
| RF14 | Enviar al dueño un correo con el resumen de la consulta, los exámenes registrados y las próximas vacunas o controles. | Notificaciones | P8 |
| RF15 | Revisar cada día las vacunas que vencen en los próximos siete días y avisar al dueño por correo. | Citas e Historial y Notificaciones | `scripts/probar_citas.py --esperar-vacuna` |

Sobre RF14: Citas guarda el nombre de cada examen, no su resultado. El aviso de "exámenes" del catálogo se cubre en el correo del resumen, que lista los exámenes hechos en la consulta.

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
- Sin inicio de sesión real: el dueño entra con su correo y el veterinario se elige de una lista. Es una decisión de alcance ([ADR 0009](adr/0009-perfil-simulado-sin-login.md)).
- Las reglas de vacunación son un calendario académico simplificado; no reemplazan el criterio del veterinario.
- Fuera de alcance (bonificaciones del catálogo): ficha en PDF, calendario visual y reportes.
