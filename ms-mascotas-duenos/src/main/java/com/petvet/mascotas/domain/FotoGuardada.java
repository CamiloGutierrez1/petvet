package com.petvet.mascotas.domain;

/**
 * Foto subida tal como está guardada: los bytes con su tipo y la versión, que cambia en cada subida. La versión
 * va en el parámetro v de la fotoUrl y es la ETag con la que el navegador revalida la foto.
 */
public record FotoGuardada(FotoMascota foto, long version) {
}
