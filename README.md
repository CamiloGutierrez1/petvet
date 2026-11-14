# PetVet

Sistema de gestión para clínica veterinaria. Proyecto final de Diseño y Arquitectura de Software.

| Parte | Responsable | Estado |
| --- | --- | --- |
| Citas e Historial e infraestructura | Jersson | Agenda, disponibilidad, agendamiento, cancelación, consulta, historial, vacunación ficticia y publicación de los tres eventos. PostgreSQL por servicio, RabbitMQ, MailHog y Docker Compose. |
| Mascotas y Dueños y Notificaciones | Pia | Pendientes. Sus bases de datos, RabbitMQ y MailHog ya están en el Compose. |
| Frontend | Camilo y Andrés Daza | Proyecto base en [frontend](frontend/README.md), con cliente HTTP, tipos de los contratos y datos simulados. Quedan por hacer las pantallas y la conexión con la API real. |

Los reportes son una bonificación y aún no están implementados.

## Arranque local del backend

Requiere Docker con Compose. Ejecuta los comandos desde la raíz del repositorio. El frontend se arranca aparte, con los pasos de su [README](frontend/README.md).

```sh
docker compose --profile app up -d --build
python3 scripts/verificar_local.py
```

El perfil `app` levanta Citas e Historial junto con la infraestructura: un PostgreSQL por servicio, RabbitMQ y MailHog. El script comprueba salud, OpenAPI, Swagger y CORS de Citas, y la disponibilidad del SMTP local.

En el Compose principal, Citas busca a Mascotas y Dueños en `ms-mascotas-duenos:8081`, el nombre y el puerto previstos para el servicio de Pia. Mientras ese servicio no esté en el Compose, las operaciones que requieren una ficha de mascota responden `503`. Para probarlas, usa la demo.

| Componente | Dirección local |
| --- | --- |
| Swagger de Citas e Historial | http://localhost:8082/swagger-ui/index.html |
| Panel RabbitMQ | http://localhost:15672 |
| Bandeja MailHog | http://localhost:8025 |

Citas expone también `/actuator/health` y `/v3/api-docs`. Para cambiar la configuración de infraestructura, copia `.env.example` a `.env`. MailHog se ejecuta en `linux/amd64`, con emulación en Mac con Apple Silicon.

### Cómo se suman los demás servicios

Mascotas y Dueños y Notificaciones se suman al crear sus carpetas: un `<module>` en el `pom.xml` raíz, un servicio con perfil `app` en `docker-compose.yml` y su puerto en `scripts/verificar_local.py`. Los puertos y las bases de datos reservados para cada uno están en la [tabla de servicios](docs/api.md#servicios-y-puertos).

## Demostración de Citas e Historial

```sh
docker compose -f docker-compose.yml -f docker-compose.demo.yml --profile app up -d --build
python3 scripts/probar_citas.py
```

La demo usa el perfil `app` completo, con Mascotas y Dueños, Citas e Historial y Notificaciones reales. El Compose de demo solo cambia dos cosas en Citas: carga un historial de ejemplo de Tobby, Misha y Kiwi, las mascotas 1, 2 y 3 de los datos iniciales de Mascotas y Dueños, con vacunas al día para Tobby y Misha, y revisa las vacunas cada minuto en lugar de una vez al día.

El script comprueba la salud de los tres servicios y de MailHog. Registra en Mascotas y Dueños un dueño con un correo nuevo y un cachorro de 58 días, agenda el primer turno libre de mañana, comprueba que el mismo turno se rechaza con `409`, consulta la cita, registra la consulta con exámenes y una cirugía y lee el historial y las próximas vacunas. Al final busca en MailHog los correos de ese dueño: la confirmación de la cita y el resumen de la consulta deben llegar en 90 segundos y saludarlo por su nombre. El recordatorio de la vacuna polivalente depende de la revisión de cada minuto, así que el script lo espera otros 90 segundos y, si no llega, solo avisa. Con `--esperar-vacuna`, el script falla si el recordatorio no llega.

El script no crea colas en RabbitMQ: los eventos llegan a las colas de Notificaciones. Si encuentra `petvet.demo.citas` o `petvet.demo.vacunas`, las colas de prueba que creaban sus versiones anteriores, las borra y lo informa. Esas colas guardan copias de eventos que nadie consume y hacen que Citas dé por entregado un evento aunque Notificaciones no tenga sus colas.

Si corriste la demo anterior, la que usaba un servicio simulado de mascotas, la base de Citas conserva citas de mascotas simuladas cuyos ids ahora son de otras mascotas reales. Para empezar limpio, ejecuta `docker compose --profile app down -v --remove-orphans` antes de levantar la demo. Ese comando borra los datos locales de todas las bases y RabbitMQ, y el contenedor `mascotas-demo` si aún existe.

Si cambias las credenciales de RabbitMQ en `.env`, exporta también `PETVET_RABBITMQ_USER` y `PETVET_RABBITMQ_PASSWORD` antes de ejecutar el script. Compose lee `.env` y Python recibe las variables de su proceso.

## Pruebas y desarrollo

```sh
./mvnw -B -ntp verify
```

Ejecuta desde la raíz con Java 21 y Docker activo. Maven Wrapper 3.9.11 descarga Maven y `verify` incluye las pruebas unitarias y las pruebas de integración de Citas con Testcontainers.

Para desarrollar desde el IDE, levanta solo la infraestructura con `docker compose up -d --wait`. Si el perfil `app` ya ocupa los puertos, detén el servicio antes de arrancar el módulo desde el IDE.

```sh
docker compose --profile app stop ms-citas-historial
./mvnw -pl ms-citas-historial spring-boot:run
```

Desde el IDE, Citas busca Mascotas y Dueños en `http://localhost:8081` salvo que configures `PETVET_MASCOTAS_URL`. Si personalizas las credenciales locales, exporta las variables correspondientes en el IDE o en la terminal. Para diagnosticar el arranque en Docker, usa `docker compose logs --tail=100 ms-citas-historial`.

## Detener los servicios

Ejecuta `docker compose --profile app down`, también después de la demo: el Compose de demo no agrega servicios, solo cambia la configuración de Citas. El comando conserva los datos en los volúmenes de Docker.

## Verificación local del 10 de noviembre de 2026

- `./mvnw -B -ntp verify` terminó correctamente con 83 pruebas unitarias y 21 de integración, sin fallos ni pruebas omitidas. Las pruebas de integración levantan PostgreSQL y RabbitMQ aislados en Testcontainers.
- `python3 scripts/verificar_local.py` comprobó salud, Swagger, OpenAPI, CORS y MailHog en Docker.
- `python3 scripts/probar_citas.py --esperar-vacuna` completó la reserva, el rechazo del duplicado con 409, la consulta, el historial y las próximas vacunas. Se recibieron `cita.agendada`, `cita.completada` y `vacuna.proxima` en las colas de demo.

La demo usa un servicio HTTP de mascotas con datos sintéticos. La integración con los servicios definitivos de Pia y el frontend del equipo sigue pendiente.

## Documentación

Consulta las rutas, el formato de los eventos y el contrato provisional con Mascotas y Dueños en [Contratos de API y eventos](docs/api.md). Las reglas de agenda, vacunación y reintentos están en [Decisiones de backend](docs/decisiones.md). Para la presentación, abre [Sustentación del backend](docs/sustentacion-backend.md).
