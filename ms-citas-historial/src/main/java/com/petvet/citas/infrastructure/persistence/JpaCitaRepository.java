package com.petvet.citas.infrastructure.persistence;

import com.petvet.citas.domain.Cita;
import com.petvet.citas.domain.CitaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaCitaRepository implements CitaRepository {
    private final SpringDataCitaRepository repository;

    public JpaCitaRepository(SpringDataCitaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Cita guardar(Cita cita) {
        return repository.saveAndFlush(CitaEntity.fromDomain(cita)).toDomain();
    }

    @Override
    public Optional<Cita> buscarPorId(long id) {
        return repository.findById(id).map(CitaEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<Cita> buscarPorIdParaActualizar(long id) {
        return repository.buscarParaActualizar(id).map(CitaEntity::toDomain);
    }

    @Override
    public List<Cita> porMascota(long mascotaId) {
        return repository.findByMascotaIdOrderByFechaHoraDescIdDesc(mascotaId).stream()
                .map(CitaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Cita> agenda(long veterinarioId, LocalDate fecha) {
        return repository
                .findByVeterinarioIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAscIdAsc(
                        veterinarioId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                .stream()
                .map(CitaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Cita> porVeterinario(long veterinarioId) {
        return repository.findByVeterinarioIdOrderByFechaHoraDescIdDesc(veterinarioId).stream()
                .map(CitaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Long> mascotasConCitas() {
        return repository.mascotasConCitas();
    }

    @Override
    public boolean horarioOcupado(long veterinarioId, LocalDateTime inicio, int duracionMinutos) {
        return repository.veterinarioOcupado(veterinarioId, inicio, inicio.plusMinutes(duracionMinutos));
    }

    @Override
    public boolean mascotaOcupada(long mascotaId, LocalDateTime inicio, int duracionMinutos) {
        return repository.mascotaOcupada(mascotaId, inicio, inicio.plusMinutes(duracionMinutos));
    }
}
