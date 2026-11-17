# Demo y plan de pruebas

Guion para mostrar PetVet desde la interfaz y lista de pruebas manuales con el sistema completo. Complementa la [sustentación del backend](sustentacion-backend.md), que recorre los patrones en Swagger y en el código.

## Preparar el entorno

Ejecuta los comandos en Git Bash desde la raíz del repositorio, con Docker Desktop abierto.

1. Solo la primera vez, o si quieres empezar con los datos limpios:

   ```sh
   docker compose --profile app down -v --remove-orphans
   ```

2. Levanta los servicios con el historial de ejemplo:

   ```sh
   docker compose -f docker-compose.yml -f docker-compose.demo.yml --profile app up -d --build
   ```

3. Abre en el navegador:

   | Qué | Dirección |
   | --- | --- |
   | PetVet | http://localhost:5173 |
   | Bandeja de correos (MailHog) | http://localhost:8025 |
   | Panel RabbitMQ (usuario y clave en `.env`) | http://localhost:15672 |

   El back solo acepta peticiones desde `http://localhost:5173`. Si Vite arranca en otro puerto, cierra el proceso que ocupa el 5173.

Los datos iniciales son:

| Dueño | Correo | Mascotas |
| --- | --- | --- |
| Laura Méndez | laura.mendez@correo.com | Tobby (perro, id 1) y Misha (gata, id 2) |
| Santiago Rojas | santiago.rojas@correo.com | Kiwi (ave, id 3) |

| Veterinario | Especialidad | Horario |
| --- | --- | --- |
| Dra. Paula Castaño | Medicina general | 8:00 a 18:00 |
| Dr. Andrés Villamil | Dermatología | 9:00 a 17:00 |
| Dra. Valentina Ríos | Cirugía | 7:00 a 13:00 |
| Dr. Julián Herrera | Odontología | 13:00 a 19:00 |
| Dra. Sofía Cárdenas | Medicina felina | 10:00 a 16:00 |

## Guion de la demo (unos 8 minutos)

Ten abiertas dos pestañas de PetVet, una para el dueño y otra para el veterinario, y una de MailHog.

1. **Arquitectura (1 min).** Muestra el diagrama de componentes: el front habla por REST con Mascotas y Dueños (8081) y con Citas e Historial (8082). Citas consulta la ficha de la mascota en Mascotas y publica eventos en RabbitMQ. Notificaciones (8083) los consume y envía los correos. Cada servicio tiene su propia base PostgreSQL.
2. **Registro del dueño (1 min).** En la entrada, elige "Soy dueño" y registra un dueño nuevo con un correo que no exista. Muestra que documento, teléfono y dirección son opcionales. Intenta registrar otra vez el mismo correo: aparece "Ya existe un dueño registrado con ese correo."
3. **Mascota (1 min).** Con ese dueño, registra una mascota con foto (JPG, PNG o WebP de hasta 5 MB) y abre su ficha.
4. **Agendar (1,5 min).** En "Agendar", elige la mascota y un veterinario. Muestra que cada veterinario tiene su horario y que solo salen los turnos libres. Agenda una cita y abre MailHog: llega el correo de confirmación con el nombre del dueño.
5. **Turno ocupado (1 min).** En la otra pestaña, con Laura, intenta agendar con el mismo veterinario a la misma hora: aparece "El veterinario ya tiene una cita en ese horario." Explica que la base de datos impide el choque aunque dos personas reserven al mismo tiempo.
6. **Consulta (1,5 min).** En la pestaña del veterinario, entra con el veterinario de la cita, ábrela desde la agenda y registra la consulta con diagnóstico, un examen y una cirugía. Abre la ficha del paciente: se ven el historial y los datos de contacto del dueño. En MailHog llega el resumen de la consulta.
7. **Tolerancia a fallos (1 min).** Ejecuta `docker compose stop ms-notificaciones`, agenda otra cita y muestra que se guarda aunque no llegue el correo. Ejecuta `docker compose start ms-notificaciones`: en menos de un minuto llega el correo pendiente. Explica que Citas guarda el evento en la outbox y lo reintenta hasta que RabbitMQ y Notificaciones lo reciben.

Si sobra tiempo, apaga Mascotas (`docker compose stop ms-mascotas-duenos`) y muestra que la lista de pacientes sigue cargando con "Mascota #N" en lugar de los nombres, y que agendar muestra un error claro. Vuelve a encenderlo con `docker compose start ms-mascotas-duenos`.

## Plan de pruebas manuales

Resultado de la ejecución del 14 de noviembre de 2026 con los tres servicios reales y el Compose de demo.

| # | Prueba | Pasos | Resultado esperado | 14 nov |
| --- | --- | --- | --- | --- |
| P1 | Datos reales en el front | Entrar como Laura y abrir la ficha de Tobby | Se ven sus mascotas, la ficha y el historial de ejemplo | OK |
| P2 | Registro de dueño | Registrar un dueño con solo nombre y correo | Se crea y los campos opcionales quedan como "sin registrar" | OK |
| P3 | Correo repetido | Registrar otra vez el mismo correo | Mensaje "Ya existe un dueño registrado con ese correo." | OK |
| P4 | Horario del veterinario | Elegir la Dra. Valentina Ríos al agendar | Solo aparecen turnos entre 7:00 y 13:00 | OK |
| P5 | Confirmación por correo | Agendar una cita | Llega a MailHog el correo de confirmación con el nombre del dueño | OK |
| P6 | Veterinario ocupado | Agendar el mismo veterinario y hora con otra mascota | Mensaje "El veterinario ya tiene una cita en ese horario." y el turno ya no aparece como libre | OK |
| P7 | Mascota ocupada | Agendar la misma mascota y hora con otro veterinario | Mensaje "La mascota ya tiene una cita en ese horario." | OK |
| P8 | Registrar consulta | Registrar diagnóstico, un examen y una cirugía, escritos sin presionar Enter | Se guardan los dos, salen en el historial y llega el correo con el resumen | OK, después del arreglo del PR #32 |
| P9 | Datos del dueño en la ficha | Abrir la ficha del paciente como veterinario | Se ven documento, teléfono y dirección, o "sin registrar" si faltan | OK |
| P10 | Cancelar cita | Cancelar una cita y volver a agendar a la misma hora | El turno vuelve a quedar libre y se puede reservar | OK |
| P11 | Notificaciones caído | Detener `ms-notificaciones`, agendar y volver a encenderlo | La cita se guarda; el correo llega al encender el servicio | OK |
| P12 | Mascotas caído | Detener `ms-mascotas-duenos`, abrir Pacientes y agendar | Pacientes muestra "Mascota #N"; agendar muestra "No se pudo conectar con el servidor." Al encenderlo todo vuelve sin reiniciar nada más | OK |

### Error encontrado y corregido

En P8, un examen y una cirugía escritos sin presionar Enter se perdían al guardar y el back recibía listas vacías. Desde el PR #32, el campo agrega el texto pendiente al salir de la caja o al presionar Guardar.

### Pruebas automáticas

Las pruebas unitarias y de integración del back (186 y 58) y el script `scripts/probar_citas.py` están descritos en el [README](../README.md#pruebas-y-desarrollo). En el front, `npm run lint` y `npm run build` revisan el código y los tipos.
