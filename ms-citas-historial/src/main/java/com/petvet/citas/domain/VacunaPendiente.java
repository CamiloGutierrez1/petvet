package com.petvet.citas.domain;

import java.time.LocalDate;

public record VacunaPendiente(String vacuna, LocalDate fechaLimite) {
}
