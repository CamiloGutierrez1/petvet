# ADR-010: Gitflow con ramas protegidas y Conventional Commits

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

Cuatro personas trabajaban al mismo tiempo en el frontend y en tres servicios del back, en un repositorio único. Hacía falta que nadie rompiera lo que ya funcionaba, que cada cambio se pudiera revisar antes de integrarse y que siempre existiera una versión estable para la demo y la entrega.

## Decisión

Usamos Gitflow simplificado. `main` guarda la versión entregable y `develop` la integración del día a día; las dos están protegidas y nadie hace push directo. Cada cambio vive en su propia rama `feature/…` o `fix/…` que sale de `develop`, se integra con un pull request y se borra después. Cuando `develop` está estable, se pasa a `main` con un pull request de entrega. Los commits siguen Conventional Commits con el servicio como alcance, por ejemplo `feat(citas): …`, `fix(front): …` o `docs: …`.

## Opciones evaluadas

- **Trunk-based (todos sobre `main`):** menos ramas y menos merges, pero sin una rama estable aparte; un commit roto deja sin demo al equipo, y requiere pruebas automáticas en cada push que no teníamos desde el primer día.
- **GitHub Flow (`main` y ramas de funcionalidad):** más simple que Gitflow, pero `main` recibe cada cambio apenas se aprueba y no hay un lugar para integrar y probar el front con los servicios antes de entregar.
- **Gitflow completo (con ramas `release/…` y `hotfix/…`):** pensado para varias versiones en paralelo; con una sola entrega, el pull request de `develop` a `main` cumple el papel de la rama de release.

## Consecuencias

- **A favor:** `main` siempre tuvo una versión que funcionaba. Cada cambio quedó revisado en un pull request (37 en total entre el 10 y el 16 de noviembre). Los mensajes con alcance muestran en el historial qué servicio cambió y por qué.
- **En contra:** más pasos por cambio (rama, pull request, merge) y ramas que se desactualizan si no se integran pronto; dos pull requests de Pia quedaron sin integrar porque el backend que tocaban cambió de dueño (ver la sección de Git del informe).
