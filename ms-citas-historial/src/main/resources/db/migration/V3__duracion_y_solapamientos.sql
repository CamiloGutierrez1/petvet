CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE citas
    ADD COLUMN duracion_minutos INTEGER NOT NULL DEFAULT 30,
    ADD CONSTRAINT citas_duracion_valida CHECK (duracion_minutos BETWEEN 1 AND 480);

ALTER TABLE citas
    ADD CONSTRAINT citas_veterinario_sin_solapamientos
    EXCLUDE USING gist (
        veterinario_id WITH =,
        (tsrange(fecha_hora, fecha_hora + duracion_minutos * INTERVAL '1 minute', '[)')) WITH &&
    ) WHERE (estado <> 'CANCELADA');

ALTER TABLE citas
    ADD CONSTRAINT citas_mascota_sin_solapamientos
    EXCLUDE USING gist (
        mascota_id WITH =,
        (tsrange(fecha_hora, fecha_hora + duracion_minutos * INTERVAL '1 minute', '[)')) WITH &&
    ) WHERE (estado <> 'CANCELADA');
