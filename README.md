# PetVet

Sistema de gestión para clínica veterinaria. Proyecto final de Diseño y Arquitectura de Software.

| Parte | Responsable | Estado |
| --- | --- | --- |
| Citas e Historial e infraestructura | Jersson | Agenda con el horario de cada veterinario, disponibilidad, agendamiento, cancelación, consulta con exámenes y cirugías, historial, vacunación ficticia y publicación de los tres eventos. PostgreSQL por servicio, RabbitMQ, MailHog y Docker Compose. |
| Mascotas y Dueños | Jersson | Registro y consulta de dueños y mascotas, fotos y la ficha que consume Citas. |
| Notificaciones | Jersson | Consume los tres eventos de Citas y envía un correo por evento, sin duplicados. |
| Frontend | Camilo y Andrés Daza | Vistas del dueño y del veterinario en [frontend](frontend/README.md). `docker compose up` las sirve en http://localhost:5173 contra la API real. |

Los reportes son una bonificación y aún no están implementados.

## Arranque local

Requiere Docker con Compose. Ejecuta los comandos desde la raíz del repositorio. En Windows, ejecútalos en Git Bash y usa `py` o `python` si `python3` no existe.

Si alguna vez levantaste la demo con el servicio simulado de mascotas (`mascotas-demo`), ejecuta una vez `docker compose --profile app down -v --remove-orphans` antes del primer arranque integrado: los volúmenes viejos conservan las colas de prueba `petvet.demo.*` y citas, historial y recordatorios de vacunas de mascotas simuladas cuyos ids ahora son de mascotas reales. Si no, el cachorro que registra `probar_citas.py` puede recibir el id 4 del Max simulado, heredar su historial y quedarse sin el recordatorio de la vacuna. El comando borra los datos locales de las bases y de RabbitMQ, y el contenedor `mascotas-demo` si aún existe.

```sh
cp .env.example .env   # solo la primera vez: usuarios, contraseñas y configuración
docker compose up -d --build
python3 scripts/verificar_local.py
```

Las credenciales de las bases y de RabbitMQ no están escritas en el Compose ni en los Dockerfile: salen de `.env`, que no se sube al repositorio (`.env.example` tiene valores ficticios solo para local). Si falta `.env`, Compose se detiene con el mensaje "copia .env.example a .env".

Un solo comando levanta los 9 contenedores: Mascotas y Dueños, Citas e Historial, Notificaciones y el frontend, junto con la infraestructura: un PostgreSQL por servicio, RabbitMQ y MailHog. Los servicios arrancan en orden gracias a los health checks: cada servicio Java espera a que su base (y RabbitMQ, si lo usa) esté sana. `docker compose ps` muestra `healthy` en los 9 cuando todo está listo, en uno o dos minutos. Los comandos anteriores con `--profile app` siguen funcionando igual.

Si en Windows la construcción falla con `additional privileges requested: pass "--allow=fs.read=..."` (Docker Compose 5 construye con bake), ejecuta `BUILDX_BAKE_ENTITLEMENTS_FS=0 docker compose up -d --build`.

Para trabajar en un servicio desde el IDE, levanta solo la infraestructura: `docker compose up -d postgres-mascotas postgres-citas postgres-notificaciones rabbitmq mailhog`. El script comprueba salud, OpenAPI, Swagger y CORS de los tres servicios, y la API y el SMTP de MailHog. Para trabajar en el frontend con recarga automática, sigue los pasos de su [README](frontend/README.md).

En el Compose, Citas consulta la ficha de cada mascota en `ms-mascotas-duenos:8081` y Notificaciones envía los correos al SMTP de MailHog. Si Mascotas y Dueños no responde, las operaciones de Citas que necesitan una ficha responden `503`.

| Componente | Dirección local |
| --- | --- |
| Frontend | http://localhost:5173 |
| Swagger de Mascotas y Dueños | http://localhost:8081/swagger-ui/index.html |
| Swagger de Citas e Historial | http://localhost:8082/swagger-ui/index.html |
| Swagger de Notificaciones | http://localhost:8083/swagger-ui/index.html |
| Panel RabbitMQ | http://localhost:15672 |
| Bandeja MailHog | http://localhost:8025 |

#### Puertos, redes y health checks

Todos los puertos se publican solo en `127.0.0.1`: el sistema no queda expuesto a la red del equipo.

| Puerto | Contenedor | Por qué se publica |
| --- | --- | --- |
| 5173 | `frontend` | Lo abre el navegador. Es el único origen que aceptan los servicios por CORS. |
| 8081, 8082 | `ms-mascotas-duenos`, `ms-citas-historial` | El front corre en el navegador y llama a la API directamente; también dan acceso a Swagger y a Postman. |
| 8083 | `ms-notificaciones` | Solo para Swagger, la salud y consultar las notificaciones enviadas; el front no lo usa. |
| 5433, 5434, 5435 | `postgres-mascotas`, `postgres-citas`, `postgres-notificaciones` | Para revisar las bases con un cliente SQL y para correr un servicio desde el IDE. No usan 5432 para no chocar con un PostgreSQL instalado en el equipo. |
| 5672, 15672 | `rabbitmq` | AMQP para correr un servicio desde el IDE y el panel para ver colas y DLQ. |
| 1025, 8025 | `mailhog` | SMTP para correr Notificaciones desde el IDE y la bandeja web donde se ven los correos. |

Redes propias (no se usa la red por defecto de Docker):

| Red | Quién la usa | Para qué |
| --- | --- | --- |
| `red-mascotas`, `red-citas`, `red-notificaciones` | Cada servicio con su base | Una base solo es visible para su servicio (base por servicio, ADR-002). |
| `red-eventos` | `rabbitmq`, Citas y Notificaciones | Publicar y consumir eventos. |
| `red-servicios` | Citas y Mascotas | Citas pide la ficha de la mascota por REST. |
| `red-correo` | `mailhog` y Notificaciones | Envío de correos por SMTP. |
| `red-frontend` | `frontend` | nginx sirve el front; el navegador llama a la API por los puertos publicados. |

Health checks: las bases usan `pg_isready`, RabbitMQ `rabbitmq-diagnostics check_running`, MailHog revisa su puerto SMTP, los tres servicios Java llaman a `/actuator/health` (incluye la base y RabbitMQ) y el frontend pide la página a nginx. Los servicios dependientes esperan con `depends_on: condition: service_healthy`. Citas no espera a Mascotas a propósito: si Mascotas cae, Citas sigue arriba y responde `503` solo en lo que necesita la ficha (ADR-005). El front no depende de nadie: se sirve aunque un servicio esté caído.

Los datos viven en los volúmenes `mascotas-data`, `citas-data`, `notificaciones-data` y `rabbitmq-data`, así que sobreviven a `docker compose down`. `docker compose down -v` los borra.

Los tres servicios exponen también `/actuator/health` y `/v3/api-docs`. El panel de RabbitMQ tiene activo el plugin de shovel: si un correo no se pudo enviar y su mensaje quedó en una DLQ, "Move messages" lo devuelve a su cola, como explica [Reprocesar una DLQ](docs/api.md#reprocesar-una-dlq). Para cambiar la configuración, edita `.env`. MailHog se ejecuta en `linux/amd64`, con emulación en Mac con Apple Silicon.

### Cómo se suma un servicio

Cada servicio se suma así: un `<module>` en el `pom.xml` raíz, un servicio con su red, su health check y su `depends_on` en `docker-compose.yml`, su puerto en `scripts/verificar_local.py` y un `Dockerfile` en su carpeta que compile solo su módulo con `mvn -B -ntp -f <modulo>/pom.xml -DskipTests package`. Con `-f`, Maven toma el `pom.xml` raíz como padre sin leer los demás módulos, así que el `Dockerfile` copia solo el `pom.xml` raíz, el del módulo y su `src`. Con `-pl` desde la raíz, la imagen dejaría de compilar en cuanto el `pom.xml` raíz liste otro módulo. Los puertos y las bases de datos de cada servicio están en la [tabla de servicios](docs/api.md#servicios-y-puertos).

Cada módulo hereda del `pom.xml` raíz las dependencias comunes (web, validation, data-jpa, el driver de PostgreSQL, actuator, springdoc y test) y declara solo las propias, por ejemplo Flyway, Testcontainers o AMQP.

## Demostración

Si antes usaste la demo con `mascotas-demo`, ejecuta primero el paso único de [Arranque local](#arranque-local).

```sh
docker compose -f docker-compose.yml -f docker-compose.demo.yml --profile app up -d --build
python3 scripts/probar_citas.py
```

La demo levanta el sistema completo, con Mascotas y Dueños, Citas e Historial y Notificaciones reales. El Compose de demo solo cambia dos cosas en Citas: carga un historial de ejemplo de Tobby, Misha y Kiwi, las mascotas 1, 2 y 3 de los datos iniciales de Mascotas y Dueños, con vacunas al día para Tobby y Misha, y revisa las vacunas cada minuto en lugar de una vez al día.

El script comprueba la salud de los tres servicios y de MailHog. Registra en Mascotas y Dueños un dueño con un correo nuevo y un cachorro de 58 días, agenda el primer turno libre de mañana, comprueba que el mismo turno se rechaza con `409`, consulta la cita, registra la consulta con exámenes y una cirugía y lee el historial y las próximas vacunas. Al final busca en MailHog los correos de ese dueño: la confirmación de la cita y el resumen de la consulta deben llegar en 90 segundos y saludarlo por su nombre. El recordatorio de la vacuna polivalente depende de la revisión de cada minuto, así que el script lo espera otros 90 segundos y, si no llega, solo avisa. Con `--esperar-vacuna`, el script falla si el recordatorio no llega.

El script no crea colas en RabbitMQ: los eventos llegan a las colas de Notificaciones. Si encuentra `petvet.demo.citas` o `petvet.demo.vacunas`, las colas de prueba que creaban sus versiones anteriores, las borra y lo informa. Esas colas guardan copias de eventos que nadie consume y hacen que Citas dé por entregado un evento aunque Notificaciones no tenga sus colas.

Si cambias las credenciales de RabbitMQ en `.env`, exporta también `PETVET_RABBITMQ_USER` y `PETVET_RABBITMQ_PASSWORD` antes de ejecutar el script. Compose lee `.env` y Python recibe las variables de su proceso.

## Pruebas y desarrollo

```sh
./mvnw -B -ntp verify
```

Ejecuta desde la raíz con Java 21 y Docker activo. El Maven Wrapper descarga Maven 3.9.11, y `verify` corre las pruebas unitarias y las de integración de los tres servicios. Las de integración usan Testcontainers para PostgreSQL en los tres servicios y para RabbitMQ en Citas y Notificaciones. Notificaciones usa además GreenMail, un SMTP en memoria.

Para desarrollar desde el IDE, levanta solo la infraestructura con `docker compose up -d --wait postgres-mascotas postgres-citas postgres-notificaciones rabbitmq mailhog`. Si los contenedores de los servicios ya ocupan los puertos, detén el servicio que vas a arrancar desde el IDE. Por ejemplo, con Citas:

```sh
docker compose --profile app stop ms-citas-historial
./mvnw -pl ms-citas-historial spring-boot:run
```

Desde el IDE, Citas busca Mascotas y Dueños en `http://localhost:8081` salvo que configures `PETVET_MASCOTAS_URL`, y Notificaciones envía los correos al SMTP de MailHog en `localhost:1025`. Si personalizas las credenciales locales, exporta las variables correspondientes en el IDE o en la terminal. Para diagnosticar el arranque en Docker, usa `docker compose logs --tail=100` con el nombre del servicio, por ejemplo `ms-citas-historial`.

## Detener los servicios

Ejecuta `docker compose --profile app down`, también después de la demo: el Compose de demo no agrega servicios, solo cambia la configuración de Citas. El comando conserva los datos en los volúmenes de Docker.

## Verificación local del 13 y 14 de noviembre de 2026

`./mvnw -B -ntp verify` terminó correctamente con 186 pruebas unitarias y 58 de integración, sin fallos ni pruebas omitidas.

| Servicio | Pruebas unitarias | Pruebas de integración |
| --- | --- | --- |
| Mascotas y Dueños | 24 | 17 |
| Citas e Historial | 108 | 32 |
| Notificaciones | 54 | 9 |

Con el Compose de demo, `python3 scripts/probar_citas.py` completó el recorrido con los tres servicios reales: rechazó con `409` el turno repetido, registró la consulta con exámenes y una cirugía, y encontró en MailHog la confirmación de la cita, el resumen de la consulta y el recordatorio de la vacuna, los tres con el nombre del dueño.

## Documentación

Consulta las rutas de los tres servicios, sus errores, el formato de los eventos, las colas de Notificaciones y cómo reprocesar una DLQ en [Contratos de API y eventos](docs/api.md). Las reglas de agenda, vacunación, reintentos y deduplicación están en [Decisiones de backend](docs/decisiones.md). Los requerimientos están en [Requerimientos](docs/requerimientos.md); los estilos arquitectónicos, los diagramas C4 de contexto y de contenedores, los atributos de calidad y los trade-offs, en [Arquitectura](docs/arquitectura.md), y cada decisión en su [ADR](docs/adr/README.md) (14 ADR). El diseño final de clases está en [Diagrama de clases](docs/diagrama-clases.md), el flujo de ramas, las convenciones y las métricas del equipo en [Flujo de trabajo con Git](docs/git.md) y los errores y mejoras en [Lecciones aprendidas](docs/lecciones-aprendidas.md). Para la presentación, abre [Sustentación del backend](docs/sustentacion-backend.md) y el guion de la demo por la interfaz con el plan de pruebas en [Demo y plan de pruebas](docs/demo-y-pruebas.md).
