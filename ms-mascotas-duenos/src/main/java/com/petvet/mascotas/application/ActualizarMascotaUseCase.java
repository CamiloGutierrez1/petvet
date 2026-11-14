package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import com.petvet.mascotas.domain.RegistroMascotas;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarMascotaUseCase {
    private final ConsultaMascotas consulta;
    private final RegistroMascotas registro;
    private final Clock clock;

    public ActualizarMascotaUseCase(ConsultaMascotas consulta, RegistroMascotas registro, Clock clock) {
        this.consulta = consulta;
        this.registro = registro;
        this.clock = clock;
    }

    /** Cambia solo los cinco campos del cuerpo. La foto y el dueño se conservan. */
    @Transactional
    public Mascota ejecutar(long id, DatosMascota datos) {
        ReglasMascota.validar(datos, LocalDate.now(clock));
        Mascota actual = consulta.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la mascota " + id));
        return registro.actualizar(new Mascota(id, datos.nombre().strip(), datos.especie(), datos.raza().strip(),
                datos.fechaNacimiento(), datos.peso(), actual.fotoUrl(), actual.duenoId()));
    }
}
