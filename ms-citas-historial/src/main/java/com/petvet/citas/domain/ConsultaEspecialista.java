package com.petvet.citas.domain;

public final class ConsultaEspecialista implements Consulta {
    @Override
    public RegistroHistorial registrar(RegistroHistorialBuilder builder) {
        return builder.tipoConsulta(TipoConsulta.ESPECIALISTA).build();
    }
}
