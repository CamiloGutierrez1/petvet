package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.AlmacenFotosMascota;
import com.petvet.mascotas.domain.FotoMascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarFotoMascotaUseCase {
    private final AlmacenFotosMascota fotos;

    public ConsultarFotoMascotaUseCase(AlmacenFotosMascota fotos) {
        this.fotos = fotos;
    }

    /** Las fotos de ejemplo las sirve el front; aquí solo están las subidas. */
    @Transactional(readOnly = true)
    public FotoMascota ejecutar(long id) {
        return fotos.buscarFoto(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay una foto subida para la mascota " + id));
    }
}
