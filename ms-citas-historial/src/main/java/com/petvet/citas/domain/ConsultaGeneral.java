package com.petvet.citas.domain;

public final class ConsultaGeneral implements Consulta {
    @Override
    public RegistroHistorial registrar(RegistroHistorialBuilder builder) {
        return builder.tipoConsulta(TipoConsulta.GENERAL).build();
    }
}
