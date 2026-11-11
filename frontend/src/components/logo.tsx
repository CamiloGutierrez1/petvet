import { cn } from "@/lib/utils"

interface LogoSimboloProps {
  className?: string
}

/**
 * Símbolo de PetVet: una placa de collar con una huella.
 * Toma los colores de los tokens (primary y primary-foreground).
 */
export function LogoSimbolo({ className }: LogoSimboloProps) {
  return (
    <svg
      viewBox="0 0 40 40"
      fill="none"
      aria-hidden
      className={cn("size-8 shrink-0", className)}
    >
      <circle
        cx="20"
        cy="7"
        r="3.6"
        strokeWidth="2.4"
        className="stroke-primary"
      />
      <circle cx="20" cy="24" r="14" className="fill-primary" />
      <g className="fill-primary-foreground">
        <ellipse cx="20" cy="28" rx="4.6" ry="3.6" />
        <circle cx="14.6" cy="22.6" r="1.9" />
        <circle cx="18" cy="19.4" r="1.9" />
        <circle cx="22" cy="19.4" r="1.9" />
        <circle cx="25.4" cy="22.6" r="1.9" />
      </g>
    </svg>
  )
}

interface LogoProps {
  className?: string
}

/** Logo completo: símbolo + "PetVet". */
export function Logo({ className }: LogoProps) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <LogoSimbolo />
      <span className="text-lg font-bold tracking-[-0.03em] text-foreground">
        PetVet
      </span>
    </span>
  )
}
