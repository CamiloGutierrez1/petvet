# Arquitectura de PetVet

Resumen de la arquitectura: estilos, diagramas C4 de contexto y de contenedores, atributos de calidad y los trade-offs de las decisiones principales. Cada decisión tiene su ADR en [adr/](adr/README.md). Los requerimientos están en [Requerimientos](requerimientos.md) y los patrones y SOLID con enlaces al código en [Sustentación del backend](sustentacion-backend.md).

## Estilos arquitectónicos

PetVet combina tres estilos, cada uno en un nivel distinto.

| Estilo | Dónde se ve | Por qué |
| --- | --- | --- |
| **Microservicios orientados a eventos** | Tres servicios con su propia base: Mascotas y Dueños, Citas e Historial, y Notificaciones. Citas publica `cita.agendada`, `cita.completada` y `vacuna.proxima` en RabbitMQ y Notificaciones los consume. | Cada parte del dominio cambia, se prueba y falla por separado. Los correos no frenan el agendamiento. [ADR 0001](adr/0001-microservicios.md), [ADR 0003](adr/0003-rabbitmq-para-notificaciones.md) |
| **Capas con puertos y adaptadores (Clean Architecture)** | Dentro de cada servicio: `presentation` (controladores REST), `application` (casos de uso), `domain` (reglas y puertos, Java sin Spring) e `infrastructure` (JPA, REST, RabbitMQ, SMTP). Las dependencias apuntan hacia el dominio. | Las reglas del negocio no dependen de la base, del broker ni del correo. Cambiar un adaptador no toca el dominio. [ADR 0006](adr/0006-clean-architecture-por-servicio.md) |
| **Cliente-servidor** | El navegador ejecuta el frontend (React) y llama por HTTP/JSON a Mascotas y Dueños y a Citas e Historial. | El front solo muestra y captura; las reglas viven en el back. [ADR 0008](adr/0008-spa-react-con-capa-api.md) |

**¿Y MVC?** No es el estilo del sistema. En el back, los controladores de Spring cumplen el papel del controlador, pero solo traducen HTTP y delegan en los casos de uso; no hay vistas en el servidor. En el front, las pantallas están separadas por funcionalidad (`features/`): la vista arma la pantalla, los hooks manejan el estado del servidor con TanStack Query y `src/api` hace las llamadas. Es una separación en capas, no MVC clásico.

## C4 nivel 1: contexto

```mermaid
flowchart TB
    dueno["<b>Dueño</b><br/>[Persona]<br/>Registra sus mascotas,<br/>agenda y cancela citas"]
    vet["<b>Veterinario</b><br/>[Persona]<br/>Atiende citas y registra<br/>el historial clínico"]
    petvet["<b>PetVet</b><br/>[Sistema]<br/>Gestión de dueños, mascotas,<br/>citas, historial y notificaciones"]
    correo["<b>Servidor de correo</b><br/>[Sistema externo]<br/>Entrega los correos al dueño.<br/>En local, MailHog"]

    dueno -- "Usa desde el navegador" --> petvet
    vet -- "Usa desde el navegador" --> petvet
    petvet -- "Envía correos de confirmación,<br/>resumen y vacunas [SMTP]" --> correo

    classDef persona fill:#1E5A4C,color:#fff,stroke:#143d34
    classDef sistema fill:#2f7d6a,color:#fff,stroke:#1E5A4C
    classDef externo fill:#8a9590,color:#fff,stroke:#5B6661
    class dueno,vet persona
    class petvet sistema
    class correo externo
```

## C4 nivel 2: contenedores

```mermaid
flowchart TB
    dueno["<b>Dueño</b><br/>[Persona]"]
    vet["<b>Veterinario</b><br/>[Persona]"]

    subgraph petvet["PetVet (Docker Compose)"]
        front["<b>Frontend</b><br/>[React 19, TypeScript, Vite / nginx]<br/>Vistas del dueño y del veterinario<br/>:5173"]
        mascotas["<b>Mascotas y Dueños</b><br/>[Spring Boot 3.5, Java 21]<br/>Dueños, mascotas, fotos y ficha<br/>:8081"]
        citas["<b>Citas e Historial</b><br/>[Spring Boot 3.5, Java 21]<br/>Agenda, consulta, historial,<br/>vacunas y outbox<br/>:8082"]
        notif["<b>Notificaciones</b><br/>[Spring Boot 3.5, Java 21]<br/>Consume eventos y envía correos<br/>sin duplicados<br/>:8083"]
        broker[("<b>RabbitMQ</b><br/>[Broker]<br/>Exchange petvet.events,<br/>colas y DLQ")]
        dbm[("<b>BD Mascotas</b><br/>[PostgreSQL 16]")]
        dbc[("<b>BD Citas</b><br/>[PostgreSQL 16]<br/>Citas, historial, outbox")]
        dbn[("<b>BD Notificaciones</b><br/>[PostgreSQL 16]<br/>messageId enviados")]
    end

    mail["<b>MailHog</b><br/>[Servidor SMTP de pruebas]<br/>:1025 / bandeja :8025"]

    dueno -- "HTTP" --> front
    vet -- "HTTP" --> front
    front -- "REST/JSON" --> mascotas
    front -- "REST/JSON" --> citas
    citas -- "REST/JSON: ficha de la mascota" --> mascotas
    citas -- "Publica eventos [AMQP]" --> broker
    broker -- "Entrega eventos [AMQP]" --> notif
    mascotas -- "JDBC" --> dbm
    citas -- "JDBC" --> dbc
    notif -- "JDBC" --> dbn
    notif -- "SMTP" --> mail

    classDef persona fill:#1E5A4C,color:#fff,stroke:#143d34
    classDef contenedor fill:#2f7d6a,color:#fff,stroke:#1E5A4C
    classDef datos fill:#E8F1EE,color:#141A18,stroke:#1E5A4C
    classDef externo fill:#8a9590,color:#fff,stroke:#5B6661
    class dueno,vet persona
    class front,mascotas,citas,notif contenedor
    class broker,dbm,dbc,dbn datos
    class mail externo
```

Notificaciones no expone rutas al frontend: solo recibe eventos. El frontend nunca habla con RabbitMQ ni con las bases.

Los dos diagramas están también como imagen para el informe y las diapositivas: [contexto](img/c4-contexto.png) y [contenedores](img/c4-contenedores.png).

## Atributos de calidad

| Atributo | Táctica en la arquitectura | Evidencia |
| --- | --- | --- |
| **Disponibilidad** | Comunicación asíncrona con RabbitMQ y patrón outbox: la cita y su evento se guardan en la misma transacción y un despachador reintenta hasta que el broker confirma. En el front, cada sección carga por separado y un servicio caído solo afecta su parte de la pantalla. | P11 y P12 en vivo. `OutboxRabbitIT` |
| **Consistencia** | Restricciones de exclusión en PostgreSQL sobre el intervalo de cada cita, por veterinario y por mascota. El servicio valida antes para dar un mensaje claro, pero la base es la garantía. | P6, P7. `CitasApiIT` con reservas simultáneas |
| **Confiabilidad** | Entrega al menos una vez con `messageId` único: Notificaciones lo registra en la misma transacción del envío y descarta repetidos. Reintentos con espera creciente y DLQ. | `NotificacionesRabbitIT` |
| **Mantenibilidad** | Capas con puertos y adaptadores en cada servicio. En el front, vistas, hooks y `src/api` separados; el front depende solo de los contratos de `types.ts`. | El front se construyó con mocks y pasó a la API real cambiando una variable de entorno |
| **Extensibilidad** | Strategy para los esquemas de vacunación (OCP). Especies y tipos de servicio en listas únicas. | `SelectorEsquemaVacunacion`, `VacunacionStrategyTest` |
| **Usabilidad** | Errores con un formato único `{error, detalle}` y el detalle en español, que el front muestra tal cual. Estados de carga, vacío y error en cada vista. | P3, P6, P7 |

## Trade-offs

Cada decisión mejora un atributo a cambio de otro. Estos son los principales; el detalle está en cada ADR.

| Decisión | Ganamos | Pagamos | ADR |
| --- | --- | --- | --- |
| Microservicios en vez de monolito | Cada servicio cambia, se prueba y falla por separado | Más infraestructura, red entre servicios y más difícil de probar de punta a punta | [0001](adr/0001-microservicios.md) |
| Una base por servicio | Cada servicio es dueño de sus datos | No hay consultas ni transacciones entre bases; Citas pide la ficha por REST | [0002](adr/0002-base-de-datos-por-servicio.md) |
| Notificaciones por eventos en RabbitMQ | Agendar no depende del correo (disponibilidad) | El correo llega segundos después (consistencia eventual) y hay un componente más que operar | [0003](adr/0003-rabbitmq-para-notificaciones.md) |
| Outbox para publicar | Ningún evento se pierde aunque el broker esté caído | Un evento puede llegar repetido: hace falta deduplicar por `messageId` | [0004](adr/0004-outbox-y-entrega-al-menos-una-vez.md) |
| REST síncrono de Citas a Mascotas | Simple y siempre con los datos actuales de la mascota | Si Mascotas cae, no se puede agendar (`503`) | [0005](adr/0005-rest-sincrono-citas-a-mascotas.md) |
| Clean Architecture en cada servicio | Dominio aislado, fácil de probar y de cambiar | Más clases e interfaces para operaciones sencillas | [0006](adr/0006-clean-architecture-por-servicio.md) |
| Choques de horario en PostgreSQL | Correcto con reservas simultáneas | Atado a una característica de PostgreSQL (restricción de exclusión) | [0007](adr/0007-choques-de-horario-en-postgresql.md) |
| SPA con capa `src/api` y mocks | El front avanzó sin esperar al back | Hay que mantener los mocks iguales al contrato real | [0008](adr/0008-spa-react-con-capa-api.md) |
| Perfil simulado sin login | Tiempo para la arquitectura y el dominio | Sin seguridad: cualquiera puede entrar como cualquier dueño | [0009](adr/0009-perfil-simulado-sin-login.md) |
