import { QueryClientProvider } from "@tanstack/react-query"
import { StrictMode, Suspense } from "react"
import { createRoot } from "react-dom/client"
import { RouterProvider } from "react-router-dom"
import { CargandoPagina } from "@/components/cargando-pagina"
import { Toaster } from "@/components/ui/sonner"
import { PerfilProvider } from "@/features/perfil/perfil-provider"
import { queryClient } from "@/lib/query-client"
import { router } from "@/routes/router"
import "./index.css"

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <PerfilProvider>
        {/* Para las pantallas sin layout que se cargan bajo demanda (ej. /registro). */}
        <Suspense
          fallback={
            <div className="mx-auto max-w-md px-4 py-20">
              <CargandoPagina />
            </div>
          }
        >
          <RouterProvider router={router} />
        </Suspense>
      </PerfilProvider>
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  </StrictMode>,
)
