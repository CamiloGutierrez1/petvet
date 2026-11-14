package com.petvet.mascotas.presentation.dto;

import com.petvet.mascotas.application.FichaMascota;
import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.Mascota;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Ficha plana de GET /api/mascotas/{id}: los campos de la mascota más el nombre y el correo del dueño. */
public record FichaMascotaResponse(Long id, String nombre, Especie especie, String raza, LocalDate fechaNacimiento,
                                   BigDecimal peso, String fotoUrl, Long duenoId, String duenoNombre,
                                   String duenoEmail) {

    public static FichaMascotaResponse desde(FichaMascota ficha) {
        Mascota mascota = ficha.mascota();
        return new FichaMascotaResponse(mascota.id(), mascota.nombre(), mascota.especie(), mascota.raza(),
                mascota.fechaNacimiento(), MascotaResponse.sinCerosSobrantes(mascota.peso()), mascota.fotoUrl(),
                mascota.duenoId(), ficha.dueno().nombre(), ficha.dueno().email());
    }
}
