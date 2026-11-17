package com.petvet.notificaciones.infrastructure.persistence;

import com.petvet.notificaciones.domain.Notificacion;
import com.petvet.notificaciones.domain.NotificacionRepository;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaNotificacionRepository implements NotificacionRepository {
    private final SpringDataNotificacionRepository repository;

    public JpaNotificacionRepository(SpringDataNotificacionRepository repository) {
        this.repository = repository;
    }

    /** Exige la transacción del caso de uso, que también cubre el envío del correo. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean registrarSiEsNueva(Notificacion notificacion) {
        return repository.insertarSiEsNueva(notificacion.messageId(), notificacion.tipo().name(),
                notificacion.destino(), notificacion.asunto(), notificacion.cuerpo(),
                notificacion.creadaEn(), notificacion.enviadaEn()) == 1;
    }

    @Override
    public List<Notificacion> recientes(int limite) {
        return repository.findAllByOrderByEnviadaEnDescIdDesc(Limit.of(limite)).stream()
                .map(NotificacionEntity::toDomain)
                .toList();
    }
}
