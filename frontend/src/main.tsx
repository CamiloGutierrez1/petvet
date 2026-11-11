import { QueryClientProvider } from "@tanstack/react-query"
import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { RouterProvider } from "react-router-dom"
import { Toaster } from "@/components/ui/sonner"
import { PerfilProvider } from "@/features/perfil/perfil-provider"
import { queryClient } from "@/lib/query-client"
import { router } from "@/routes/router"
import "./index.css"

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <PerfilProvider>
        <RouterProvider router={router} />
      </PerfilProvider>
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  </StrictMode>,
)
