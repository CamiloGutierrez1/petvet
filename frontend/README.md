# PetVet – Frontend

Aplicación web de PetVet. Es la base sobre la que Camilo construye las vistas del **dueño** y Daza las del **veterinario**. Esta base todavía no tiene pantallas reales: trae la configuración, el cliente HTTP, los tipos de los contratos, los datos simulados y las rutas.

> Estado: base lista (martes 10 nov). La paleta, la tipografía y el logo se definen la tarde del martes 10 entre Camilo y Daza.

## Cómo levantarlo

Necesitas Node 20 o superior.

```bash
cd frontend
npm install
cp .env.example .env     # en Windows (Git Bash) funciona igual
npm run dev              # abre http://localhost:5173
```

| Script                 | Qué hace                                      |
| ---------------------- | --------------------------------------------- |
| `npm run dev`          | Servidor de desarrollo con recarga automática |
| `npm run build`        | Revisa los tipos con TypeScript y compila     |
| `npm run lint`         | ESLint (errores de código y reglas de React)  |
| `npm run format`       | Prettier: formatea todo el proyecto           |
| `npm run format:check` | Prettier: solo revisa, sin cambiar nada       |

Antes de abrir un PR: `npm run lint` y `npm run build` sin errores.

### Con Docker

El `Dockerfile` compila con Node 22 y sirve el resultado con nginx (`nginx.conf`). nginx devuelve `index.html` en cualquier ruta, así que recargar en `/dueno/citas` funciona.

```bash
docker build -t petvet-frontend .
docker run --rm -p 5173:80 petvet-frontend     # http://localhost:5173
```

Las variables `VITE_*` se incrustan **al compilar**, no al arrancar. Por defecto la imagen usa la API real (`VITE_USE_MOCKS=false`) en `localhost:8081` y `localhost:8082`: son las URLs que usa el navegador, por eso no van los nombres internos de los contenedores. Para otra configuración: `docker build --build-arg VITE_USE_MOCKS=true -t petvet-frontend .`

## Stack

| Qué         | Tecnología                                                     |
| ----------- | -------------------------------------------------------------- |
| Base        | React 19 + Vite 8 + TypeScript 6                               |
| Estilos     | Tailwind CSS v4 + shadcn/ui (base Radix), íconos lucide-react  |
| Datos       | TanStack Query 5 + Axios                                       |
| Formularios | React Hook Form + Zod + `@hookform/resolvers`                  |
| Rutas       | React Router 7                                                 |
| Toasts      | sonner (ya montado en `main.tsx`)                              |
| Calidad     | ESLint + Prettier (con orden automático de clases de Tailwind) |

## Estructura

```
src/
├── api/
│   ├── http.ts        clientes Axios (mascotasApi, citasApi) e interruptor de mocks
│   ├── types.ts       tipos de los contratos (única dependencia con el back)
│   ├── mascotas.ts    funciones de ms-mascotas-duenos (8081)
│   └── citas.ts       funciones de ms-citas-historial (8082)
├── mocks/             JSON simulados con la forma de los contratos
├── components/
│   ├── ui/            componentes de shadcn (no editar a mano salvo estilos)
│   └── ...            componentes propios compartidos
├── features/
│   ├── perfil/            Camilo (selector, PerfilProvider, usePerfil, RequierePerfil)
│   ├── duenos-mascotas/   Camilo
│   ├── citas/             Camilo (agendar)
│   └── veterinario/       Daza (agenda, consulta, pacientes)
├── layouts/           LayoutDueno.tsx, LayoutVeterinario.tsx y barra-superior.tsx
├── lib/               utilidades (cn, queryClient)
├── routes/router.tsx  definición de rutas
└── main.tsx           punto de entrada: QueryClient + PerfilProvider + Router + Toaster
```

Regla: **las páginas solo arman la vista**. Las llamadas a la API van en `src/api` y se usan desde hooks con TanStack Query. Ninguna página usa Axios directamente.

## Datos: API real o datos simulados

En `.env`:

```
VITE_API_MASCOTAS=http://localhost:8081/api
VITE_API_CITAS=http://localhost:8082/api
VITE_USE_MOCKS=true
```

Con `VITE_USE_MOCKS=true`, las funciones de `src/api` responden con los JSON de `src/mocks` (con 400 ms de espera, para ver los skeletons). Con `false`, llaman a la API real. Las páginas no cambian: pasar a la API real es cambiar esa variable y reiniciar `npm run dev`.

**Un interruptor por servicio.** Para conectar un microservicio mientras el otro sigue simulado, define en `.env` los específicos (mandan sobre `VITE_USE_MOCKS`):

```
VITE_USE_MOCKS_MASCOTAS=true    # Mascotas simulado (mientras Pia termina)
VITE_USE_MOCKS_CITAS=false      # Citas contra el servicio real de Jersson
```

Para probar con Citas real hay que levantar la demo de Jersson desde la raíz del repo (sin el front, para no ocupar el 5173 de `npm run dev`):

```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml --profile app --profile demo up -d --build ms-citas-historial mascotas-demo
```

La demo trae un servicio de mascotas de prueba con los ids 1, 2 y 3, los mismos de nuestros mocks, así que se puede agendar para Tobby (1) y Misha (2). Una mascota registrada en el front (id 4 en adelante) no existe en esa demo y Citas responde 404 al agendarle.

En modo mocks, lo que se registra (mascotas, citas, consultas) queda en memoria hasta recargar la página, así que se pueden probar los flujos completos.

Los mocks de Citas imitan las reglas del servicio real de Jersson (`docs/api.md` y `docs/decisiones.md` en la raíz): turnos de 30 minutos de 8:00 a 18:00, sin horas pasadas, **409** si el turno ya está ocupado o si se cancela una cita completada, y **404** si el recurso no existe. Los errores simulados tienen la misma forma que los reales, así que el manejo de errores se puede probar sin el back.

### Errores de la API

Todos los errores llegan como `{ "error": "CODIGO", "detalle": "Mensaje en español" }`:

| HTTP | `error`                     | Cuándo                                              |
| ---- | --------------------------- | --------------------------------------------------- |
| 400  | `DATOS_INVALIDOS`           | Datos o fechas inválidos                            |
| 404  | `RECURSO_NO_ENCONTRADO`     | No existe la cita, mascota, veterinario, etc.       |
| 409  | `CONFLICTO`                 | Horario ocupado o estado de cita incompatible       |
| 503  | `DEPENDENCIA_NO_DISPONIBLE` | Citas no pudo consultar a Mascotas (servicio caído) |
| 500  | `ERROR_INTERNO`             | Error no previsto                                   |

**El mensaje para el usuario está en `detalle`**, no en `error` (que es un código para el programa). Para leerlo usa `mensajeDeError(error, "Texto por defecto")` de `src/api/errores.ts`: devuelve el `detalle`, un aviso de conexión si el servidor no responde, o el texto por defecto. `estadoHttp(error)` devuelve el código (409, 404, 503...).

**Manejo global de errores** (`src/lib/query-client.ts`, con el `onError` de TanStack Query, no un interceptor de Axios: en modo mocks los errores no pasan por Axios). Las vistas muestran sus errores en su lugar (`EstadoError` con "Reintentar", recuadros en formularios); el aviso global (toast) cubre lo demás:

- **Acciones (`useMutation`)**: si fallan, sale un toast con el `detalle` del back, automáticamente. Si tu vista ya muestra el error (en un formulario o diálogo), marca la acción con `meta: { errorEnPantalla: true }` para que no salga dos veces.
- **Consultas (`useQuery`)**: en la primera carga el error lo muestra la vista; si la consulta ya tenía datos y falla al actualizarse, sale un toast.

### Funciones disponibles

`src/api/mascotas.ts`: `registrarDueno`, `getDuenos`, `getDueno`, `registrarMascota`, `getMascotasDeDueno`, `getMascota` (ficha con nombre y correo del dueño), `editarMascota`, `subirFotoMascota`.

`src/api/citas.ts`: `getVeterinarios`, `getDisponibilidad`, `agendarCita`, `getCitasDeMascota`, `getAgendaVeterinario`, `cancelarCita`, `registrarConsulta`, `getHistorial`, `getProximasVacunas`.

**Para las vistas del veterinario (Daza):** `getAgendaVeterinario(veterinarioId, "2026-11-20")`, `registrarConsulta(citaId, datos)`, `getHistorial(mascotaId)`, `getProximasVacunas(mascotaId)`, `getMascota(id)`, `cancelarCita(id)` y `getVeterinarios()`. Los mocks tienen citas para el **20 de noviembre** (veterinarios 1 y 2).

Ojo con la disponibilidad: `getDisponibilidad` devuelve un objeto `{ veterinarioId, fecha, horarios }`, no un arreglo. Las horas libres están en `horarios`.

Ejemplo de uso en un componente:

```tsx
import { useQuery } from "@tanstack/react-query"
import { getAgendaVeterinario } from "@/api/citas"

const {
  data: citas,
  isLoading,
  error,
  refetch,
} = useQuery({
  queryKey: ["agenda", veterinarioId, fecha],
  queryFn: () => getAgendaVeterinario(veterinarioId, fecha),
})
// isLoading → skeleton · error → mensaje claro con "Reintentar" · [] → estado vacío
```

### Formatos compartidos (`src/lib/formato.ts`)

Para que las dos mitades del front muestren fechas y textos igual:

| Función                          | Ejemplo                                                                                                 |
| -------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `formatearFecha("2026-12-12")`   | `12 dic 2026`                                                                                           |
| `formatearFechaCorta(...)`       | `12 dic`; con año si no es el actual: `12 nov 2023`                                                     |
| `formatearHora(fechaHora)`       | `9:30 a. m.`                                                                                            |
| `formatearFechaHora(fechaHora)`  | `vie 20 nov 2026, 9:30 a. m.`                                                                           |
| `textoPlazo("2026-12-12")`       | `en 31 días`, `mañana`, `hoy`, `venció hace 3 días`, `venció hace 2 meses`, `venció hace más de un año` |
| `etiquetaServicio("VACUNACION")` | `Vacunación` (y `TIPOS_SERVICIO` para un `select`)                                                      |

### Tipos

Todo lo que viaja entre front y back está en `src/api/types.ts` (`Mascota`, `Cita`, `RegistrarConsulta`, `RegistroHistorial`, etc.). Los tipos de Citas ya están confirmados con el servicio de Jersson. Los campos de Mascotas marcados con `// confirmar con back` siguen pendientes con Pia: si cambian, se ajustan **solo ahí** y TypeScript marca todo lo que haya que corregir.

## Rutas

Están en `src/routes/router.tsx`. Las vistas que aún no existen muestran una página provisional; para construir una, se reemplaza `<PaginaPendiente ... />` por el componente real.

| Ruta                                | Vista                                        | Responsable | Estado    |
| ----------------------------------- | -------------------------------------------- | ----------- | --------- |
| `/`                                 | Entrada: rol + correo (dueño) o lista (vet.) | Camilo      | Listo     |
| `/registro`                         | Registro de dueño (fuera de los layouts)     | Camilo      | Listo     |
| `/dueno`                            | Inicio: resumen con secciones "Ver todas"    | Camilo      | Listo     |
| `/dueno/mascotas`                   | Mis mascotas                                 | Camilo      | Listo     |
| `/dueno/mascotas/:mascotaId`        | Ficha de la mascota                          | Camilo      | Listo     |
| `/dueno/agendar`                    | Agendar cita                                 | Camilo      | Listo     |
| `/dueno/citas`                      | Mis citas                                    | Camilo      | Listo     |
| `/veterinario/agenda`               | Agenda del día                               | Daza        | Listo     |
| `/veterinario/consulta/:citaId`     | Registrar consulta                           | Daza        | Listo     |
| `/veterinario/pacientes`            | Pacientes (lista, destino del menú)          | Daza        | Pendiente |
| `/veterinario/pacientes/:mascotaId` | Ficha del paciente con historial             | Daza        | Listo     |

Si necesitas otra ruta, agrégala en tu sección del router y menciónalo en el PR.

### Perfil activo y layouts

- **Perfil:** no hay login real (decisión del proyecto). En `/` (`EntradaPage`, diseño V4 en dos columnas con `LayoutEntrada`) se elige Dueño o Veterinario: el dueño entra con su **correo** (se busca en `GET /api/duenos`; si no existe, se ofrece el registro) y el veterinario se elige de la lista. Queda guardado en el navegador; si ya hay perfil, aparece "Continuar como…". En cualquier vista: `const { perfil } = usePerfil()` → `perfil.tipo`, `perfil.id`, `perfil.nombre` (`src/features/perfil/perfil-context.ts`).
- **Protección:** `RequierePerfil` devuelve al selector si se entra a `/dueno/...` o `/veterinario/...` sin el perfil correspondiente.
- **Layouts:** `LayoutDueno` y `LayoutVeterinario` (`src/layouts/`) comparten `BarraSuperior`: logo, enlaces (el activo con `bg-brand-soft text-primary`), acción principal y menú del perfil con "Cambiar perfil". En celular los enlaces pasan a un panel lateral (`sheet`). El contenido va centrado con ancho máximo de 1200px.
- **Las vistas no ponen su propio margen ni barra**: el layout ya lo hace. Empiezan con `<EncabezadoPagina titulo="..." />`.

## Componentes de shadcn/ui

Instalados en `src/components/ui/`: avatar, badge, button, calendar, card, dialog, dropdown-menu, field, input, label, select, separator, skeleton, sonner, table y textarea.

Para agregar otro: `npx shadcn@latest add <nombre>` desde `frontend/`. Avisa en el grupo para no instalar lo mismo dos veces.

**Componentes propios compartidos** (`src/components/`): `EstadoCitaBadge`, `TablaDatos`, `EstadoVacio`, `EstadoError`, `CargandoSkeleton`, `EncabezadoPagina` (de Daza), `Logo`, `PaginaPendiente` y `ListaTextos` (lista de textos con etiquetas: se escribe, Enter y queda como etiqueta que se puede quitar; acepta `sugerencias`. Se usa en Registrar consulta para vacunas, tratamientos y exámenes).

**Formularios:** la versión actual de shadcn ya no trae el componente `form`; lo reemplazó por **`field`** (`Field`, `FieldLabel`, `FieldError`, etc.). Los formularios se hacen con React Hook Form + Zod para el estado y la validación, y `field` para la parte visual. Los dos usamos esta misma forma.

## Estilo y diseño

- Colores y tipografía son **tokens** en `src/index.css` (Tailwind v4 ya no usa `tailwind.config`). Se usan con clases como `bg-primary`, `text-muted-foreground`, `border-border`. **Nada de colores sueltos** (`bg-[#12a4b0]`, `text-teal-600`).
- Siempre manejar carga (skeleton), vacío y error. Nunca pantallas en blanco.

### Identidad de PetVet

Tipografía única: **Plus Jakarta Sans** (400, 500, 600, 700), cargada desde Google Fonts en `index.html`. Solo modo claro.

| Token (clase)                           | Color                           | Uso                                                  |
| --------------------------------------- | ------------------------------- | ---------------------------------------------------- |
| `primary` / `primary-foreground`        | `#1E5A4C` / `#FFFFFF`           | Botones principales, enlaces, foco (`ring`)          |
| `background`                            | `#F7F8F7`                       | Fondo de la app                                      |
| `card`, `popover`                       | `#FFFFFF`                       | Tarjetas, menús, diálogos                            |
| `foreground` / `muted-foreground`       | `#141A18` / `#5B6661`           | Texto principal / secundario                         |
| `border` / `input`                      | `#E3E8E5` / `#D5DDD9`           | Bordes de tarjetas / de inputs y botones secundarios |
| `brand-soft`                            | `#E8F1EE`                       | Menú activo, avatares, selección                     |
| `alerta`, `alerta-soft`, `alerta-texto` | `#B86E00`, `#FFF3DF`, `#7A4900` | **Solo** vacunas pendientes                          |
| `estado-agendada` / `-soft`             | `#24508F` / `#E5EDFA`           | `EstadoCitaBadge`                                    |
| `estado-completada` / `-soft`           | `#1E6B4A` / `#E3F1EB`           | `EstadoCitaBadge`                                    |
| `estado-cancelada` / `-soft`            | `#5B6661` / `#EFF1F0`           | `EstadoCitaBadge`                                    |

Radios: 10px por defecto (`rounded-lg`) y 16px en tarjetas (`rounded-xl`).

Logo: `<Logo />` (símbolo + "PetVet") o `<LogoSimbolo />` (solo la placa con la huella), en `src/components/logo.tsx`. El mismo símbolo es el favicon (`public/favicon.svg`).

Ejemplos: `bg-brand-soft text-primary` (enlace activo), `bg-alerta-soft text-alerta-texto` (aviso de vacuna), `text-muted-foreground` (texto secundario).

- Espaciado en múltiplos de 4. Sin degradados morados, emojis como íconos ni textos de relleno.

## Editor y formato

- Al abrir el repo, VS Code sugiere las extensiones: ESLint, Prettier, Tailwind CSS IntelliSense y Live Share. Instálalas.
- `.vscode/settings.json` (en la raíz del repo) formatea con Prettier al guardar. Así los dos producimos el mismo formato y no hay conflictos falsos en Git.
- Importa con el alias `@/` en vez de rutas relativas largas: `import { Button } from "@/components/ui/button"`.

## Decisiones tomadas en la base (y por qué)

| Decisión                                                                   | Motivo                                                                                                                                                                                                                      |
| -------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| TypeScript 6.0 en vez del 7 que trae Vite                                  | `typescript-eslint` aún no soporta TS 7, y el plan pide ESLint. Para un proyecto de este tamaño no hay diferencia práctica.                                                                                                 |
| ESLint + Prettier en vez de oxlint                                         | Es lo acordado en el plan; ESLint tiene las reglas de React Hooks.                                                                                                                                                          |
| `field` en vez de `form` de shadcn                                         | `form` ya no existe en la versión actual de shadcn.                                                                                                                                                                         |
| shadcn con base Radix y preset Nova                                        | Radix es la base más documentada; Nova trae lucide como librería de íconos.                                                                                                                                                 |
| Paquete `cn` (de shadcn) en `src/lib/utils.ts`                             | Así lo genera la versión actual de shadcn; reemplaza a `clsx` + `tailwind-merge`.                                                                                                                                           |
| Pantallas cargadas bajo demanda (`React.lazy` en `src/routes/paginas.tsx`) | La primera carga baja de 886 KB a 383 KB: cada pantalla se descarga solo al visitarla. Mientras llega, el layout muestra `CargandoPagina`.                                                                                  |
| `npm audit` muestra avisos                                                 | Todos vienen de herramientas de desarrollo (ESLint, shadcn CLI). En el código que llega al navegador hay 0 vulnerabilidades (`npm audit --omit=dev`). No ejecutar `npm audit fix --force`: baja shadcn a una versión vieja. |

## Pendientes y coordinación

| Qué                                                                                            | Quién          | Cuándo           |
| ---------------------------------------------------------------------------------------------- | -------------- | ---------------- |
| ~~Paleta, tipografía y logo como tokens en `src/index.css`~~ (hecho, PR #5)                    | Camilo y Daza  | Mar 10           |
| ~~Barra superior, selector de perfil y rutas con layouts~~ (hecho, `feature/front-layout`)     | Camilo         | Mié 11           |
| ~~Componentes compartidos (badge de estado de cita, tabla, skeletons)~~ (hecho, PR #4)         | Daza           | Mié 11           |
| ~~Manejo global de errores (toast con el `detalle`)~~ (hecho en `src/lib/query-client.ts`)     | Daza / Camilo  | Jue 12           |
| Cerrar los campos `// confirmar con back` de Mascotas en `types.ts` (Citas ya está confirmado) | Camilo con Pia | Antes del sáb 14 |
| Pasar a la API real (`VITE_USE_MOCKS=false`)                                                   | Camilo y Daza  | Sáb 14           |

## Flujo de Git

- Rama por tarea desde `develop`: `feature/front-<tarea>` (por ejemplo `feature/front-agenda-veterinario`).
- Commits pequeños: `feat(front): ...`, `fix(front): ...`, `chore: ...`.
- PR hacia `develop`; Camilo (líder) revisa y aprueba los PR del equipo el mismo día.
- Nunca subir `.env`, `node_modules/` ni `dist/` (ya están en el `.gitignore`).
