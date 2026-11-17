package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarMascotasUseCase {
    private final ConsultaMascotas mascotas;
    private final ConsultaDuenos duenos;

    public ConsultarMascotasUseCase(ConsultaMascotas mascotas, ConsultaDuenos duenos) {
        this.mascotas = mascotas;
        this.duenos = duenos;
    }

    /** Ficha de GET /api/mascotas/{id}: la mascota con el nombre y el correo de su dueño. La consume Citas. */
    @Transactional(readOnly = true)
    public FichaMascota ficha(long id) {
        Mascota mascota = mascotas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la mascota " + id));
        // La llave foránea garantiza el dueño. Si faltara, es un error interno y no un 404.
        Dueno dueno = duenos.buscarPorId(mascota.duenoId())
                .orElseThrow(() -> new IllegalStateException("La mascota " + id + " no tiene dueño"));
        return new FichaMascota(mascota, dueno);
    }

    @Transactional(readOnly = true)
    public List<Mascota> deDueno(long duenoId) {
        if (duenos.buscarPorId(duenoId).isEmpty()) {
            throw new RecursoNoEncontradoException("No existe el dueño " + duenoId);
        }
        return mascotas.buscarPorDueno(duenoId);
    }
}
