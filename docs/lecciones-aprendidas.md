# Errores, dificultades y lecciones aprendidas

Lo que salió mal durante el proyecto, por qué pasó y qué haríamos distinto.

## Errores técnicos

### 1. Los exámenes y cirugías escritos sin Enter se perdían

| Campo | Detalle |
| --- | --- |
| Error | Al registrar una consulta, si el veterinario escribía un examen o una cirugía y pulsaba "Guardar" sin presionar Enter, ese texto no se guardaba. |
| Contexto | Frontend, pantalla Registrar consulta, componente `ListaTextos`. Se encontró en la prueba real contra los tres servicios el 14 de noviembre. |
| Causa raíz | El componente solo agregaba una etiqueta al presionar Enter. Con los mocks nadie lo notó porque siempre probábamos presionando Enter. |
| Solución | El componente agrega lo escrito también al salir del campo (pull request #32). |
| Lección | Probar con flujos reales de usuario y contra el back real, no solo con los datos simulados. |

### 2. Campos que pedía el catálogo y no estaban en el contrato

| Campo | Detalle |
| --- | --- |
| Error | A mitad del desarrollo faltaban datos que el catálogo exigía: documento y dirección del dueño, horario propio de cada veterinario y cirugías en la consulta. |
| Contexto | Contrato de la API entre el front y los servicios (`docs/api.md`), Mascotas y Dueños y Citas e Historial. |
| Causa raíz | El contrato inicial se armó desde las pantallas, no desde el catálogo; nadie cruzó los dos documentos al empezar. |
| Solución | Revisión del catálogo punto por punto el 12 de noviembre, contrato actualizado en un solo lugar (`docs/api.md`) y cambios en el back (#29) y en el front (#31), con migraciones nuevas de Flyway (`V6__cirugias`, `V7__horarios_veterinarios`). |
| Lección | Cruzar requerimientos y contrato antes de construir, y mantener una única fuente del contrato. |

### 3. Un recordatorio de vacuna terminaba en la mascota equivocada

| Campo | Detalle |
| --- | --- |
| Error | Al pasar de la demo con Mascotas simulado a Mascotas real, una mascota nueva heredaba el historial y las citas de una mascota simulada y no recibía su recordatorio de vacuna. |
| Contexto | Docker Compose, volúmenes de la base de Citas y de RabbitMQ. |
| Causa raíz | Los volúmenes viejos guardaban citas e historial de mascotas simuladas con ids 1, 2, 3 y 4; la base real reutilizó esos ids para mascotas distintas. |
| Solución | `docker compose down -v` una vez antes del primer arranque integrado, documentado en el README. Los datos de demo pasaron a una carpeta aparte (`db/demo`) que solo se carga a propósito. |
| Lección | Separar los datos de ejemplo de los reales y no reutilizar volúmenes cuando cambia el origen de los ids. |

### 4. El front no recibía respuesta del back (CORS)

| Campo | Detalle |
| --- | --- |
| Error | El navegador bloqueaba las llamadas a la API con un error de CORS. |
| Contexto | Frontend con `npm run dev` y servicios en Docker. |
| Causa raíz | Un proceso viejo de Vite ocupaba el puerto 5173, así que el nuevo se abrió en el 5174; los servicios solo aceptan el origen `http://localhost:5173`. |
| Solución | Cerrar el proceso viejo y documentar el puerto obligatorio en el README del front. |
| Lección | Documentar los supuestos del entorno (puertos y orígenes permitidos) desde el principio. |

### 5. La construcción con Docker Compose 5 fallaba en Windows

| Campo | Detalle |
| --- | --- |
| Error | `docker compose up --build` se detenía con `additional privileges requested: pass "--allow=fs.read=frontend\Dockerfile"`. |
| Contexto | Equipo de Camilo con Docker Desktop actualizado (Compose 5.6, que construye con bake), al probar el Compose final el 20 de noviembre. |
| Causa raíz | Bake revisa los permisos de lectura de cada archivo y, con la ruta de Windows del proyecto, pedía un permiso extra para el Dockerfile del front. |
| Solución | `BUILDX_BAKE_ENTITLEMENTS_FS=0 docker compose up -d --build`, documentado en el README. |
| Lección | Probar el arranque desde cero en una máquina limpia antes de cada entrega, no solo cuando se cambia el Compose. |

## Dificultades del proceso de equipo

- **Redistribución del trabajo del back.** Mascotas y Dueños y Notificaciones estaban asignados a Pia. Al 13 de noviembre no había avances que se pudieran integrar, así que Jersson construyó los dos servicios (#27 y #28) además de Citas. Los 5 pull requests de Pia se cerraron sin merge: tres eran versiones del servicio de Mascotas, que Jersson terminó con sus pruebas en el #27, y dos traían archivos de front en JavaScript que duplicaban pantallas existentes. Debimos detectarlo antes, con avances diarios visibles en el repositorio.
- **Revisión de código concentrada en una persona.** Camilo revisó todos los pull requests de los demás, pero los suyos se integraron sin otro revisor (opción "bypass" de GitHub) y solo 2 pull requests tienen aprobación formal en GitHub. Fue más rápido, pero perdimos una segunda mirada sobre la mitad del código.
- **Front sin back durante tres días.** Los servicios llegaron entre el 10 y el 14 de noviembre. El front avanzó con datos simulados que tenían la misma forma del contrato ([ADR-008](adr/ADR-008-spa-react-con-capa-api.md)); al llegar cada servicio solo hubo que cambiar una variable de entorno.
- **Cambios de diseño a mitad de camino.** El horario pasó de ser uno solo para toda la clínica (8:00 a 18:00) a uno por veterinario, y el dueño ganó documento y dirección. Gracias a Flyway y a las capas, el cambio quedó en migraciones nuevas, el dominio de Citas y el formulario del front, sin tocar Notificaciones.
- **Deuda técnica que dejamos:** no hay inicio de sesión ([ADR-009](adr/ADR-009-perfil-simulado-sin-login.md)); los recordatorios de vacuna solo cubren mascotas que tuvieron al menos una cita; el front no tiene pruebas automáticas; los exámenes guardan el nombre pero no el resultado.

## Sugerencias de mejora

1. **Técnica: copia local de las mascotas en Citas.** Hoy, si Mascotas cae, no se puede agendar (`503`, [ADR-005](adr/ADR-005-rest-sincrono-citas-a-mascotas.md)). Mascotas podría publicar un evento `mascota.registrada` y Citas guardar una copia de los datos que necesita, para agendar aunque Mascotas esté caído.
2. **Proceso: tablero con issues y revisión cruzada.** Cada tarea como issue en GitHub Projects enlazada a su pull request, y cada pull request aprobado por alguien distinto a su autor, sin "bypass". Así los avances y los bloqueos se ven a diario.
3. **Calidad: pruebas automáticas del front y de punta a punta.** Vitest para las funciones de formato y Playwright para los flujos de agendar y registrar la consulta, ejecutados en GitHub Actions en cada pull request.
4. **Trabajo futuro:** inicio de sesión con JWT y roles (dueño y veterinario), la ficha de la mascota en PDF, el calendario visual de citas y los reportes de la clínica (las bonificaciones del catálogo).
