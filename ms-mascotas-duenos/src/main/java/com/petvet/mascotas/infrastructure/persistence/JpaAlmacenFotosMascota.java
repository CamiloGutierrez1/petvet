package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.AlmacenFotosMascota;
import com.petvet.mascotas.domain.FotoGuardada;
import com.petvet.mascotas.domain.FotoMascota;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda las fotos en PostgreSQL: bytes y tipo en fotos_mascotas, versión en mascotas.foto_version.
 * Así sobreviven a reconstruir la imagen y el contenedor no necesita escribir en disco.
 */
@Repository
@Transactional(readOnly = true)
public class JpaAlmacenFotosMascota implements AlmacenFotosMascota {
    private final SpringDataFotoMascotaRepository fotos;
    private final SpringDataMascotaRepository mascotas;
    private final EntityManager entityManager;

    public JpaAlmacenFotosMascota(SpringDataFotoMascotaRepository fotos, SpringDataMascotaRepository mascotas,
            EntityManager entityManager) {
        this.fotos = fotos;
        this.mascotas = mascotas;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void actualizarFoto(long mascotaId, FotoMascota foto, long versionPropuesta) {
        MascotaEntity mascota = mascotas.findById(mascotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la mascota " + mascotaId));
        // Relee la mascota con SELECT ... FOR UPDATE: dos subidas simultáneas (un doble clic) se atienden en
        // orden. La segunda reemplaza la foto de la primera en vez de chocar con su clave, y ve su versión.
        entityManager.refresh(mascota, LockModeType.PESSIMISTIC_WRITE);
        Long anterior = mascota.fotoVersion();
        mascota.cambiarVersionFoto(anterior == null ? versionPropuesta : Math.max(versionPropuesta, anterior + 1));
        fotos.saveAndFlush(new FotoMascotaEntity(mascotaId, foto));
    }

    @Override
    public Optional<FotoGuardada> buscarFoto(long mascotaId) {
        return fotos.findById(mascotaId).map(FotoMascotaEntity::aDominio);
    }
}
