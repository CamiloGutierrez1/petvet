package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import com.petvet.mascotas.domain.RegistroMascotas;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Un solo adaptador JPA implementa los dos puertos pequeños de mascotas. */
@Repository
@Transactional(readOnly = true)
public class JpaMascotaRepository implements ConsultaMascotas, RegistroMascotas {
    private final SpringDataMascotaRepository repository;

    public JpaMascotaRepository(SpringDataMascotaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Mascota> buscarPorId(long id) {
        return repository.findById(id).map(MascotaEntity::aDominio);
    }

    @Override
    public List<Mascota> buscarPorDueno(long duenoId) {
        return repository.findByDuenoIdOrderByIdAsc(duenoId).stream().map(MascotaEntity::aDominio).toList();
    }

    @Override
    @Transactional
    public Mascota guardar(Mascota mascota) {
        return repository.saveAndFlush(MascotaEntity.nueva(mascota)).aDominio();
    }

    @Override
    @Transactional
    public Mascota actualizar(Mascota mascota) {
        MascotaEntity entity = repository.findById(mascota.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la mascota " + mascota.id()));
        entity.actualizarDatos(mascota);
        return repository.saveAndFlush(entity).aDominio();
    }
}
