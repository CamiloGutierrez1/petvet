package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import com.petvet.mascotas.domain.RegistroMascotas;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarMascotaUseCase {
    private final RegistroMascotas registro;
    private final ConsultaDuenos duenos;
    private final Clock clock;

    public RegistrarMascotaUseCase(RegistroMascotas registro, ConsultaDuenos duenos, Clock clock) {
        this.registro = registro;
        this.duenos = duenos;
        this.clock = clock;
    }

    /** Registra la mascota a nombre del dueño de la ruta. Un dueño inexistente responde 404. */
    @Transactional
    public Mascota ejecutar(long duenoId, DatosMascota datos) {
        ReglasMascota.validar(datos, LocalDate.now(clock));
        duenos.buscarPorId(duenoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el dueño " + duenoId));
        return registro.guardar(new Mascota(null, datos.nombre().strip(), datos.especie(), datos.raza().strip(),
                datos.fechaNacimiento(), datos.peso(), null, duenoId));
    }
}
