package com.petvet.citas.domain;

import java.util.List;

public interface HistorialRepository {
    RegistroHistorial guardar(RegistroHistorial registro);

    List<RegistroHistorial> porMascota(long mascotaId);

}
