# ADR 0008: SPA en React con capa de API y mocks

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

El frontend tenía que avanzar mientras los servicios del back aún no existían: Mascotas y Dueños y Notificaciones llegaron el 13 y el 14 de noviembre. Además debía verse profesional, manejar bien los errores y poder explicarse en la sustentación.

## Decisión

Una SPA en React 19 con TypeScript y Vite, organizada así:

- `src/api`: las únicas funciones que llaman al back (Axios), tipadas con los contratos de `types.ts`. Con `VITE_USE_MOCKS=true` devuelven datos simulados con la misma forma, incluidos los errores.
- Hooks por funcionalidad con TanStack Query: caché, reintentos y estados de carga y error.
- Vistas en `features/` que solo arman la pantalla.
- Errores con el formato del back (`{error, detalle}`): la vista muestra el `detalle` en su sitio y lo demás sale como aviso global.
- Code splitting por ruta con `React.lazy`.

## Alternativas consideradas

- **Llamar a la API desde cada componente con `fetch`:** más directo, pero repite la lógica de errores y de carga y acopla las vistas al back.
- **Renderizado en el servidor (Next.js o plantillas de Spring):** innecesario para una aplicación interna sin requisitos de SEO, y mezcla el front con el back.

## Consecuencias

- **A favor:** el front se construyó completo con mocks y pasó a la API real cambiando una variable de entorno, sin tocar las vistas. Un servicio caído solo afecta su sección de la pantalla. La primera carga bajó de 886 KB a 383 KB.
- **En contra:** hay que mantener los mocks iguales al contrato real; cuando el contrato cambió (documento, horarios, cirugías) hubo que actualizar tipos, mocks y vistas a la vez. El back solo acepta peticiones desde `http://localhost:5173` (CORS).
