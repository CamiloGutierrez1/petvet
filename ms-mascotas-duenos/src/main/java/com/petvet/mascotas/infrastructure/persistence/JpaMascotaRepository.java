package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.ConsultaMascotas;
import com.petvet.mascotas.domain.Mascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import com.petvet.mascotas.domain.RegistroMascotas;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Un solo adaptador JPA implementa los dos puertos pequeños de mascotas. */
@Repository
@Transactional(readOnly = true)
public class JpaMascotaRepository implements ConsultaMascotas, RegistroMascotas {
    private final SpringDataMascotaRepository repository;
    private final String urlBaseFotos;

    public JpaMascotaRepository(SpringDataMascotaRepository repository,
            @Value("${petvet.fotos.url-base}") String urlBaseFotos) {
        this.repository = repository;
        this.urlBaseFotos = urlBaseFotos.endsWith("/")
                ? urlBaseFotos.substring(0, urlBaseFotos.length() - 1) : urlBaseFotos;
    }

    @Override
    public Optional<Mascota> buscarPorId(long id) {
        return repository.findById(id).map(this::aDominio);
    }

    @Override
    public List<Mascota> buscarPorDueno(long duenoId) {
        return repository.findByDuenoIdOrderByIdAsc(duenoId).stream().map(this::aDominio).toList();
    }

    @Override
    @Transactional
    public Mascota guardar(Mascota mascota) {
        return aDominio(repository.saveAndFlush(MascotaEntity.nueva(mascota)));
    }

    @Override
    @Transactional
    public Mascota actualizar(Mascota mascota) {
        MascotaEntity entity = repository.findById(mascota.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la mascota " + mascota.id()));
        entity.actualizarDatos(mascota);
        return aDominio(repository.saveAndFlush(entity));
    }

    /**
     * Una foto subida se sirve en este servicio: la fotoUrl absoluta se arma con la versión guardada
     * y la base configurada, y nunca se persiste. Sin foto subida queda la de ejemplo, relativa al front.
     */
    private Mascota aDominio(MascotaEntity entity) {
        Mascota mascota = entity.aDominio();
        if (entity.fotoVersion() == null) {
            return mascota;
        }
        return mascota.conFoto(urlBaseFotos + "/api/mascotas/" + mascota.id() + "/foto?v=" + entity.fotoVersion());
    }
}
