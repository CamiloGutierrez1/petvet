-- Cirugías registradas en la consulta. Son una lista más del historial, igual que los tratamientos
-- y los exámenes; no son un tipo de servicio ni de consulta.
CREATE TABLE historial_cirugias (
    historial_id BIGINT NOT NULL REFERENCES registros_historial (id) ON DELETE CASCADE,
    posicion INTEGER NOT NULL,
    valor TEXT NOT NULL,
    PRIMARY KEY (historial_id, posicion)
);
