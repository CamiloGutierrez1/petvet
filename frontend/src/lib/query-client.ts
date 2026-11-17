import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { mensajeDeError } from "@/api/errores"

// Opciones propias que las consultas y acciones pueden declarar en `meta`.
declare module "@tanstack/react-query" {
  interface Register {
    mutationMeta: {
      /**
       * true si la vista ya muestra el error de esta acción (un recuadro en
       * el formulario o el diálogo). Así el aviso global no lo repite.
       */
      errorEnPantalla?: boolean
    }
  }
}

/** Aviso de error; el id evita apilar el mismo mensaje varias veces. */
function avisarError(mensaje: string) {
  toast.error(mensaje, { id: mensaje })
}

// Configuración común de TanStack Query para toda la app.
//
// Manejo global de errores: las vistas muestran sus propios errores en su
// lugar (EstadoError con "Reintentar", recuadros en los formularios). Aquí
// solo se avisa lo que de otra forma pasaría en silencio:
// - una consulta que falla al ACTUALIZARSE cuando la pantalla ya tiene datos
//   (en la primera carga el error lo muestra la propia vista);
// - una acción (mutación) cuya vista no muestra el error.
export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => {
      if (query.state.data !== undefined) {
        avisarError(
          mensajeDeError(error, "No se pudieron actualizar los datos."),
        )
      }
    },
  }),
  mutationCache: new MutationCache({
    onError: (error, _variables, _contexto, mutation) => {
      if (mutation.meta?.errorEnPantalla) return
      avisarError(mensajeDeError(error, "No se pudo completar la acción."))
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 30_000, // 30 s antes de volver a pedir los mismos datos
      retry: 1, // un reintento ante fallos de red
      refetchOnWindowFocus: false,
    },
  },
})
