package com.petvet.mascotas.application;

import java.time.LocalDate;

/** Reglas comunes al registrar y al editar una mascota. */
final class ReglasMascota {
    /** Límite técnico: ninguna mascota real nace antes, y evita fechas que PostgreSQL no admite. */
    static final LocalDate NACIMIENTO_MINIMO = LocalDate.of(1900, 1, 1);

    private ReglasMascota() {
    }

    /** {@code hoy} es la fecha actual en la zona de la clínica, la misma con la que Citas valida la ficha. */
    static void validar(DatosMascota datos, LocalDate hoy) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos de la mascota son obligatorios.");
        }
        if (vacio(datos.nombre()) || vacio(datos.raza()) || datos.especie() == null) {
            throw new IllegalArgumentException("El nombre, la especie y la raza de la mascota son obligatorios.");
        }
        if (datos.fechaNacimiento() == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
        }
        if (datos.fechaNacimiento().isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        if (datos.fechaNacimiento().isBefore(NACIMIENTO_MINIMO)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser anterior a 1900.");
        }
        if (datos.peso() == null || datos.peso().signum() <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor que 0.");
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
