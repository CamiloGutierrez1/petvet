package com.petvet.citas.application;

import com.petvet.citas.domain.TipoConsulta;
import java.time.LocalDate;
import java.util.List;

public record DatosConsulta(TipoConsulta tipoConsulta, String diagnostico,
        List<String> vacunasAplicadas, List<String> tratamientos, List<String> examenes,
        List<String> cirugias, String observaciones, LocalDate proximoControl) { }
