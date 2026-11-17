package com.petvet.mascotas.domain;

import java.util.Optional;

/**
 * Puerto de las fotos subidas. Guarda bytes, tipo de contenido y versión, y los devuelve para servirlos.
 * Nunca guarda una URL: la fotoUrl se arma al responder.
 */
public interface AlmacenFotosMascota {
    /**
     * Reemplaza la foto de la mascota. La versión forma parte de la fotoUrl: se guarda la propuesta o, si la
     * foto anterior ya tiene una igual o mayor, la siguiente, para que la URL cambie en cada subida.
     */
    void actualizarFoto(long mascotaId, FotoMascota foto, long versionPropuesta);

    /** La foto subida con su versión, leídas juntas: la versión corresponde siempre a esos bytes. */
    Optional<FotoGuardada> buscarFoto(long mascotaId);
}
