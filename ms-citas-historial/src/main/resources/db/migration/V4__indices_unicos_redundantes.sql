-- Las restricciones de exclusión de V3 ya impiden dos citas vigentes en el mismo inicio para un
-- veterinario o una mascota. Estos índices únicos de V1 repetían esa regla y encarecían cada escritura.
-- Las búsquedas por veterinario o mascota y fecha siguen usando citas_veterinario_fecha_idx y citas_mascota_fecha_idx.
DROP INDEX citas_veterinario_horario_unico;
DROP INDEX citas_mascota_horario_unico;
