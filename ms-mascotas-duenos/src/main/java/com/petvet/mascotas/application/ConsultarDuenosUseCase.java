package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarDuenosUseCase {
    private final ConsultaDuenos duenos;

    public ConsultarDuenosUseCase(ConsultaDuenos duenos) {
        this.duenos = duenos;
    }

    /** Todos los dueños, ordenados por id. La entrada del front busca aquí el correo. */
    @Transactional(readOnly = true)
    public List<Dueno> listar() {
        return duenos.listar();
    }

    @Transactional(readOnly = true)
    public Dueno buscar(long id) {
        return duenos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el dueño " + id));
    }
}
