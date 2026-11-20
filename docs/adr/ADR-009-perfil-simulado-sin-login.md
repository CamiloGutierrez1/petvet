# ADR-009: Perfil simulado sin inicio de sesión

- **Estado:** Aceptada
- **Fecha:** 10 de noviembre de 2026

## Contexto

El catálogo no pide autenticación y el tiempo del proyecto es corto. Aun así, el sistema necesita saber si quien lo usa es un dueño o un veterinario, y cuál.

## Decisión

La entrada tiene un selector de perfil. El dueño se identifica con su correo, que se busca en `GET /api/duenos`; el veterinario se elige de la lista de `GET /api/veterinarios`. El perfil elegido se guarda en el navegador y se lee en un solo lugar del front (`usePerfil`). Los servicios no validan identidad.

## Opciones evaluadas

- **Login con usuario y contraseña (Spring Security y JWT):** necesario en un sistema real, pero agrega una capa de seguridad en los tres servicios sin aportar a lo que evalúa el curso.
- **Proveedor externo (OAuth):** depende de internet y de cuentas externas en una demo local.

## Consecuencias

- **A favor:** el tiempo se dedicó a la arquitectura, los patrones y los atributos de calidad. Como el perfil vive en un solo lugar, agregar un login real cambiaría la pantalla de entrada y la configuración de los servicios, no las vistas.
- **En contra:** no hay seguridad: cualquiera puede entrar como cualquier dueño o veterinario. Es aceptable solo porque el sistema corre en local para la demo.
