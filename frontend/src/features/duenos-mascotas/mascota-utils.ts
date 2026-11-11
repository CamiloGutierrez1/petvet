import type { Especie } from "@/api/types"

// Textos del dominio para mostrar los datos de una mascota.

export const ESPECIES: { valor: Especie; etiqueta: string }[] = [
  { valor: "PERRO", etiqueta: "Perro" },
  { valor: "GATO", etiqueta: "Gato" },
  { valor: "AVE", etiqueta: "Ave" },
  { valor: "OTRO", etiqueta: "Otro" },
]

export function etiquetaEspecie(especie: Especie): string {
  return ESPECIES.find((e) => e.valor === especie)?.etiqueta ?? especie
}

/** Meses cumplidos entre la fecha de nacimiento ("2021-04-18") y hoy. */
export function mesesDeEdad(fechaNacimiento: string, hoy = new Date()): number {
  const [anio, mes, dia] = fechaNacimiento.split("-").map(Number)
  let meses = (hoy.getFullYear() - anio!) * 12 + (hoy.getMonth() + 1 - mes!)
  if (hoy.getDate() < dia!) meses -= 1
  return Math.max(0, meses)
}

/** Edad legible: "8 meses", "1 año y 3 meses", "5 años". */
export function edadTexto(fechaNacimiento: string, hoy = new Date()): string {
  const meses = mesesDeEdad(fechaNacimiento, hoy)
  if (meses < 1) return "Menos de 1 mes"
  if (meses < 12) return meses === 1 ? "1 mes" : `${meses} meses`
  const anios = Math.floor(meses / 12)
  const resto = meses % 12
  if (anios === 1 && resto > 0) {
    return `1 año y ${resto} ${resto === 1 ? "mes" : "meses"}`
  }
  return anios === 1 ? "1 año" : `${anios} años`
}

const kg = new Intl.NumberFormat("es-CO", { maximumFractionDigits: 1 })

/** Peso legible: "31,5 kg"; por debajo de 1 kg en gramos: "40 g". */
export function pesoTexto(pesoKg: number): string {
  if (pesoKg < 1) return `${Math.round(pesoKg * 1000)} g`
  return `${kg.format(pesoKg)} kg`
}
