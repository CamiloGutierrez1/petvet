package com.petvet.citas.application;

import com.petvet.citas.domain.*;
import com.petvet.citas.domain.eventos.VacunaProximaEvento;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class VacunasService {
    private final HistorialRepository historiales;
    private final MascotaClient mascotas;
    private final SelectorEsquemaVacunacion selector;
    private final PublicadorEventos eventos;
    private final Clock clock;
    private final TransactionOperations transacciones;
    private final int diasAviso;

    public VacunasService(HistorialRepository historiales, MascotaClient mascotas,
            SelectorEsquemaVacunacion selector, PublicadorEventos eventos, Clock clock,
            TransactionOperations transacciones,
            @Value("${petvet.vacunas.dias-aviso}") int diasAviso) {
        this.historiales = historiales;
        this.mascotas = mascotas;
        this.selector = selector;
        this.eventos = eventos;
        this.clock = clock;
        this.transacciones = transacciones;
        this.diasAviso = diasAviso;
        if (diasAviso < 0) {
            throw new IllegalArgumentException("Los días de aviso deben ser positivos o cero");
        }
    }

    public List<RegistroHistorial> historial(long mascotaId) {
        mascotas.buscar(mascotaId);
        return historiales.porMascota(mascotaId);
    }

    public List<VacunaPendiente> proximas(long mascotaId) {
        return selector.proximasVacunas(mascotas.buscar(mascotaId), historiales.porMascota(mascotaId),
                LocalDate.now(clock));
    }

    /**
     * La ficha y el historial se leen sin transacción de escritura. Solo se abre una cuando hay
     * recordatorios dentro de la ventana de aviso, y la outbox descarta los que ya existen.
     */
    public void publicarRecordatorios(long mascotaId) {
        Mascota mascota = mascotas.buscar(mascotaId);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limite = hoy.plusDays(diasAviso);
        List<VacunaPendiente> porAvisar = selector
                .proximasVacunas(mascota, historiales.porMascota(mascotaId), hoy).stream()
                .filter(vacuna -> !vacuna.fechaLimite().isBefore(hoy) && !vacuna.fechaLimite().isAfter(limite))
                .toList();
        if (porAvisar.isEmpty()) {
            return;
        }
        transacciones.executeWithoutResult(estado -> porAvisar.forEach(vacuna ->
                eventos.vacunaProxima(new VacunaProximaEvento(mascotaId, mascota.nombre(), mascota.duenoNombre(),
                        mascota.duenoEmail(), vacuna.vacuna(), vacuna.fechaLimite()))));
    }
}
