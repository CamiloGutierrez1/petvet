CREATE TABLE outbox_eventos (
    id UUID PRIMARY KEY,
    clave_deduplicacion TEXT NOT NULL UNIQUE,
    routing_key VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_en TIMESTAMPTZ,
    intentos INTEGER NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    proximo_intento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX outbox_eventos_pendientes
    ON outbox_eventos (proximo_intento, creado_en)
    WHERE enviado_en IS NULL;
