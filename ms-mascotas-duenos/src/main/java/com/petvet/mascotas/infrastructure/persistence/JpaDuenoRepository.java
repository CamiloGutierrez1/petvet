package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.CorreoRepetidoException;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.RegistroDuenos;
import java.util.List;
import java.util.Optional;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Un solo adaptador JPA implementa los dos puertos pequeños de dueños. */
@Repository
@Transactional(readOnly = true)
public class JpaDuenoRepository implements ConsultaDuenos, RegistroDuenos {
    /** Restricción UNIQUE del correo definida en V1__mascotas_duenos.sql. */
    static final String RESTRICCION_CORREO_UNICO = "duenos_email_unico";

    private final SpringDataDuenoRepository repository;

    public JpaDuenoRepository(SpringDataDuenoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Dueno> buscarPorId(long id) {
        return repository.findById(id).map(DuenoEntity::aDominio);
    }

    @Override
    public Optional<Dueno> buscarPorCorreo(String correo) {
        return repository.findByEmail(correo).map(DuenoEntity::aDominio);
    }

    @Override
    public List<Dueno> listar() {
        return repository.findAllByOrderByIdAsc().stream().map(DuenoEntity::aDominio).toList();
    }

    @Override
    @Transactional
    public Dueno guardar(Dueno dueno) {
        try {
            return repository.saveAndFlush(DuenoEntity.nuevo(dueno)).aDominio();
        } catch (DataIntegrityViolationException error) {
            // Dos registros simultáneos con el mismo correo pasan la consulta previa: la base rechaza el segundo.
            String causa = NestedExceptionUtils.getMostSpecificCause(error).getMessage();
            if (causa != null && causa.contains(RESTRICCION_CORREO_UNICO)) {
                throw new CorreoRepetidoException();
            }
            throw error;
        }
    }
}
