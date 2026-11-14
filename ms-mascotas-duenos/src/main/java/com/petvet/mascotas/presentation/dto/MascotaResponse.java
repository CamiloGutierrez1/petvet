package com.petvet.mascotas.presentation.dto;

import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.Mascota;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Mascota en las respuestas. fotoUrl es null si no tiene foto. */
public record MascotaResponse(Long id, String nombre, Especie especie, String raza, LocalDate fechaNacimiento,
                              BigDecimal peso, String fotoUrl, Long duenoId) {

    public static MascotaResponse desde(Mascota mascota) {
        return new MascotaResponse(mascota.id(), mascota.nombre(), mascota.especie(), mascota.raza(),
                mascota.fechaNacimiento(), sinCerosSobrantes(mascota.peso()), mascota.fotoUrl(), mascota.duenoId());
    }

    /** NUMERIC(6,2) devuelve 31.50; la API responde 31.5, como los datos de ejemplo del front, y 100 en vez de 1E+2. */
    static BigDecimal sinCerosSobrantes(BigDecimal peso) {
        if (peso == null) {
            return null;
        }
        BigDecimal sinCeros = peso.stripTrailingZeros();
        return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
    }
}
