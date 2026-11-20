# ADR-013: Docker Compose para levantar el sistema en local

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

El sistema necesita tres bases PostgreSQL, RabbitMQ, un servidor de correo de pruebas, tres servicios Java y el frontend. Cada integrante usa un equipo distinto con Windows o macOS, y el sistema se presenta en local, sin despliegue en la nube. Instalar y configurar todo a mano en cada equipo era lento y propenso a diferencias de versión.

## Decisión

Todo el sistema se define en un `docker-compose.yml`. Cada servicio Java y el frontend tienen un Dockerfile multi-stage: se compila con Maven y JDK 21 (o Node 22) y se ejecuta con una imagen que solo trae el JRE 21 (o nginx). Las credenciales salen de variables de entorno documentadas en `.env.example`. Cada contenedor tiene health check, las bases y RabbitMQ tienen volúmenes persistentes, y los servicios esperan a sus dependencias con `depends_on: condition: service_healthy`. Las redes son propias: cada base solo es visible para su servicio, y RabbitMQ solo para los servicios que publican o consumen eventos.

## Opciones evaluadas

- **Instalar PostgreSQL, RabbitMQ y Java en cada equipo:** sin Docker, pero con versiones distintas en cada máquina y horas de configuración.
- **Una sola base y un solo contenedor:** más simple, pero contradice la base por servicio ([ADR-002](ADR-002-base-de-datos-por-servicio.md)) y no muestra la arquitectura real.
- **Kubernetes (minikube o kind):** más cercano a producción, pero demasiada complejidad para una demo local de una semana.

## Consecuencias

- **A favor:** un solo comando levanta las 9 piezas del sistema con las mismas versiones en todos los equipos. Las imágenes de ejecución no traen el JDK ni Maven. Con las redes separadas, un servicio no puede conectarse por error a la base de otro. Las pruebas de caída (P11 y P12) se hacen con `docker compose stop`.
- **En contra:** la primera construcción tarda varios minutos y Docker Desktop consume bastante memoria. Las URLs del front se fijan al compilar la imagen, así que apuntan a `localhost`.
