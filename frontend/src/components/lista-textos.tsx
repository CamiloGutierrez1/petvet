import { useState, type KeyboardEvent } from "react"
import { Plus, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

interface ListaTextosProps {
  id: string
  valor: string[]
  onChange: (valor: string[]) => void
  placeholder?: string
  /** Opciones rápidas que se agregan con un clic (ej. vacunas pendientes). */
  sugerencias?: string[]
  maxElementos?: number
  maxLargo?: number
  invalido?: boolean
}

/**
 * Lista de textos cortos: se escribe, se presiona Enter (o "Agregar") y
 * queda como una etiqueta que se puede quitar. Para vacunas, tratamientos,
 * exámenes, etc.
 */
export function ListaTextos({
  id,
  valor,
  onChange,
  placeholder,
  sugerencias = [],
  maxElementos = 50,
  maxLargo = 200,
  invalido = false,
}: ListaTextosProps) {
  const [texto, setTexto] = useState("")
  const lleno = valor.length >= maxElementos

  function agregar(nuevo: string) {
    const limpio = nuevo.trim().slice(0, maxLargo)
    if (!limpio || lleno) return
    // Sin repetidos (sin importar mayúsculas).
    if (!valor.some((v) => v.toLowerCase() === limpio.toLowerCase())) {
      onChange([...valor, limpio])
    }
    setTexto("")
  }

  function alPresionar(evento: KeyboardEvent<HTMLInputElement>) {
    if (evento.key === "Enter") {
      evento.preventDefault() // que Enter no envíe el formulario
      agregar(texto)
    }
  }

  const sugerenciasLibres = sugerencias.filter(
    (s) => !valor.some((v) => v.toLowerCase() === s.toLowerCase()),
  )

  return (
    <div className="flex flex-col gap-2">
      <div className="flex gap-2">
        <Input
          id={id}
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          onKeyDown={alPresionar}
          placeholder={placeholder}
          maxLength={maxLargo}
          disabled={lleno}
          aria-invalid={invalido}
        />
        <Button
          type="button"
          variant="outline"
          onClick={() => agregar(texto)}
          disabled={!texto.trim() || lleno}
        >
          <Plus />
          Agregar
        </Button>
      </div>

      {sugerenciasLibres.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="text-muted-foreground">Sugerencias:</span>
          {sugerenciasLibres.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => agregar(s)}
              className="rounded-full border border-dashed px-2.5 py-0.5 text-sm transition-colors hover:border-primary hover:text-primary"
            >
              + {s}
            </button>
          ))}
        </div>
      )}

      {valor.length > 0 && (
        <ul className="flex flex-wrap gap-2" aria-label="Agregados">
          {valor.map((v) => (
            <li
              key={v}
              className={cn(
                "flex items-center gap-1 rounded-full bg-brand-soft py-0.5 pr-1 pl-3 text-sm text-primary",
              )}
            >
              {v}
              <button
                type="button"
                onClick={() => onChange(valor.filter((x) => x !== v))}
                aria-label={`Quitar ${v}`}
                className="rounded-full p-0.5 hover:bg-primary/10"
              >
                <X className="size-3.5" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
