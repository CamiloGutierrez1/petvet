package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.AlmacenFotosMascota;
import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.FotoMascota;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubirFotoMascotaUseCase {
    private final ConsultaMascotas mascotas;
    private final AlmacenFotosMascota fotos;
    private final Clock clock;

    public SubirFotoMascotaUseCase(ConsultaMascotas mascotas, AlmacenFotosMascota fotos, Clock clock) {
        this.mascotas = mascotas;
        this.fotos = fotos;
        this.clock = clock;
    }

    /**
     * Valida la foto, la guarda con una versión nueva y devuelve la mascota con su fotoUrl absoluta.
     * La versión propuesta son los milisegundos del reloj: cambia la URL en cada subida para que el navegador no
     * muestre la foto anterior, también después de vaciar la base.
     */
    @Transactional
    public Mascota ejecutar(long id, byte[] contenido, String tipoDeclarado) {
        FotoMascota foto = FotoMascota.desde(contenido, tipoDeclarado);
        mascotas.buscarPorId(id).orElseThrow(() -> mascotaInexistente(id));
        fotos.actualizarFoto(id, foto, clock.millis());
        return mascotas.buscarPorId(id).orElseThrow(() -> mascotaInexistente(id));
    }

    private static RecursoNoEncontradoException mascotaInexistente(long id) {
        return new RecursoNoEncontradoException("No existe la mascota " + id);
    }
}
