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

Cada servicio nuevo se suma así: un `<module>` en el `pom.xml` raíz, un servicio con perfil `app` en `docker-compose.yml`, su puerto en `scripts/verificar_local.py` y un `Dockerfile` en su carpeta que compile solo su módulo con `mvn -B -ntp -f <modulo>/pom.xml -DskipTests package`. Con `-f`, Maven toma el `pom.xml` raíz como padre sin leer los demás módulos, así que el `Dockerfile` copia solo el `pom.xml` raíz, el del módulo y su `src`. Con `-pl` desde la raíz, la imagen dejaría de compilar en cuanto el `pom.xml` raíz liste otro módulo. Los puertos y las bases de datos reservados para cada uno están en la [tabla de servicios](docs/api.md#servicios-y-puertos).

Cada módulo hereda del `pom.xml` raíz las dependencias comunes (web, validation, data-jpa, el driver de PostgreSQL, actuator, springdoc y test) y declara solo las propias, por ejemplo Flyway, Testcontainers o AMQP.

## Demostración de Citas e Historial

```sh
docker compose -f docker-compose.yml -f docker-compose.demo.yml --profile app --profile demo up -d --build
python3 scripts/probar_citas.py
```

El Compose de demo dirige las consultas de Citas a `mascotas-demo:8080`, con las mascotas del front (Tobby, Misha y Kiwi) y un cachorro, Max, cuya primera vacuna vence en los próximos días. Carga además un historial de ejemplo con vacunas al día para las tres primeras y ejecuta la revisión de vacunas cada minuto. El script comprueba disponibilidad, creación de una cita, rechazo de un turno duplicado, registro de consulta, historial y próximas vacunas. Espera hasta 90 segundos por los dos eventos de cita. Para comprobar también `vacuna.proxima`, ejecuta `python3 scripts/probar_citas.py --esperar-vacuna`.

Los eventos llegan a colas de prueba en RabbitMQ, que permiten demostrar el flujo hasta el broker mientras se integra el consumidor de Pia.

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

Para el arranque principal, ejecuta `docker compose --profile app down`. Si levantaste la demo, usa los dos archivos y los dos perfiles también al detenerla.

```sh
docker compose -f docker-compose.yml -f docker-compose.demo.yml --profile app --profile demo down
```

Estos comandos conservan los datos en los volúmenes de Docker.

## Verificación local del 10 de noviembre de 2026

- `./mvnw -B -ntp verify` terminó correctamente con 83 pruebas unitarias y 21 de integración, sin fallos ni pruebas omitidas. Las pruebas de integración levantan PostgreSQL y RabbitMQ aislados en Testcontainers.
- `python3 scripts/verificar_local.py` comprobó salud, Swagger, OpenAPI, CORS y MailHog en Docker.
- `python3 scripts/probar_citas.py --esperar-vacuna` completó la reserva, el rechazo del duplicado con 409, la consulta, el historial y las próximas vacunas. Se recibieron `cita.agendada`, `cita.completada` y `vacuna.proxima` en las colas de demo.

La demo usa un servicio HTTP de mascotas con datos sintéticos. La integración con los servicios definitivos de Pia y el frontend del equipo sigue pendiente.

## Documentación

Consulta las rutas, el formato de los eventos y el contrato provisional con Mascotas y Dueños en [Contratos de API y eventos](docs/api.md). Las reglas de agenda, vacunación y reintentos están en [Decisiones de backend](docs/decisiones.md). Para la presentación, abre [Sustentación del backend](docs/sustentacion-backend.md).
