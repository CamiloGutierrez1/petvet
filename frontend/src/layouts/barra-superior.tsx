import { useState, type ReactNode } from "react"
import { Link, NavLink, useNavigate } from "react-router-dom"
import { ArrowLeftRight, ChevronDown, Menu } from "lucide-react"
import { Logo } from "@/components/logo"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { iniciales, usePerfil } from "@/features/perfil/perfil-context"
import { cn } from "@/lib/utils"

export interface EnlaceNav {
  to: string
  etiqueta: string
  /** true si solo debe marcarse activo en la ruta exacta (ej. "Inicio"). */
  exacto?: boolean
}

interface BarraSuperiorProps {
  inicio: string
  enlaces: EnlaceNav[]
  /** Texto junto al logo, ej. "Clínica" en la vista del veterinario. */
  etiqueta?: string
  /** Botón principal a la derecha, ej. "Agendar cita". */
  accion?: ReactNode
}

const ROL = { dueno: "Dueño", veterinario: "Veterinario" } as const

/**
 * Barra superior común a los dos perfiles: logo, enlaces de navegación,
 * acción principal y menú del perfil. En celular los enlaces pasan a un
 * panel lateral (Sheet).
 */
export function BarraSuperior({
  inicio,
  enlaces,
  etiqueta,
  accion,
}: BarraSuperiorProps) {
  const [menuAbierto, setMenuAbierto] = useState(false)

  return (
    <header className="sticky top-0 z-40 border-b bg-card">
      <div className="mx-auto flex h-16 w-full max-w-[1200px] items-center gap-4 px-4 sm:px-8">
        <Sheet open={menuAbierto} onOpenChange={setMenuAbierto}>
          <SheetTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              aria-label="Abrir menú"
            >
              <Menu />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="gap-0 p-0">
            <SheetHeader className="border-b p-4">
              <SheetTitle asChild>
                <div>
                  <MarcaPetVet etiqueta={etiqueta} />
                </div>
              </SheetTitle>
              <SheetDescription className="sr-only">
                Navegación principal
              </SheetDescription>
            </SheetHeader>
            <nav aria-label="Principal" className="flex flex-col gap-1 p-4">
              {enlaces.map((enlace) => (
                <EnlaceBarra
                  key={enlace.to}
                  enlace={enlace}
                  onClick={() => setMenuAbierto(false)}
                  className="h-10"
                />
              ))}
            </nav>
            {accion && (
              <div
                className="border-t p-4"
                onClick={() => setMenuAbierto(false)}
              >
                {accion}
              </div>
            )}
          </SheetContent>
        </Sheet>

        <Link to={inicio} aria-label="Ir al inicio" className="rounded-md">
          <MarcaPetVet etiqueta={etiqueta} />
        </Link>

        <nav
          aria-label="Principal"
          className="ml-4 hidden items-center gap-1 md:flex"
        >
          {enlaces.map((enlace) => (
            <EnlaceBarra key={enlace.to} enlace={enlace} />
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          {accion && <div className="hidden sm:block">{accion}</div>}
          <MenuPerfil />
        </div>
      </div>
    </header>
  )
}

function MarcaPetVet({ etiqueta }: { etiqueta?: string }) {
  return (
    <span className="flex items-center gap-2">
      <Logo />
      {etiqueta && (
        <span className="rounded-md bg-brand-soft px-2 py-0.5 text-xs font-semibold text-primary">
          {etiqueta}
        </span>
      )}
    </span>
  )
}

interface EnlaceBarraProps {
  enlace: EnlaceNav
  onClick?: () => void
  className?: string
}

function EnlaceBarra({ enlace, onClick, className }: EnlaceBarraProps) {
  return (
    <NavLink
      to={enlace.to}
      end={enlace.exacto}
      onClick={onClick}
      className={({ isActive }) =>
        cn(
          "flex h-9 items-center rounded-md px-3 text-sm font-medium transition-colors",
          isActive
            ? "bg-brand-soft text-primary"
            : "text-muted-foreground hover:bg-muted hover:text-foreground",
          className,
        )
      }
    >
      {enlace.etiqueta}
    </NavLink>
  )
}

function MenuPerfil() {
  const { perfil } = usePerfil()
  const navigate = useNavigate()
  if (!perfil) return null

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="h-10 gap-2 px-2"
          aria-label={`Perfil de ${perfil.nombre}`}
        >
          <Avatar className="size-8">
            <AvatarFallback className="bg-brand-soft text-xs font-semibold text-primary">
              {iniciales(perfil.nombre)}
            </AvatarFallback>
          </Avatar>
          <span className="hidden max-w-40 truncate text-sm font-medium lg:inline">
            {perfil.nombre}
          </span>
          <ChevronDown className="size-4 text-muted-foreground" aria-hidden />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          <span className="truncate font-semibold">{perfil.nombre}</span>
          <span className="text-xs font-normal text-muted-foreground">
            {ROL[perfil.tipo]}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => navigate("/")}>
          <ArrowLeftRight />
          Cambiar perfil
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
