-- Versión de la foto subida. La fotoUrl absoluta se arma al responder con petvet.fotos.url-base y esta versión.
ALTER TABLE mascotas ADD COLUMN foto_version BIGINT;

-- Bytes de la foto subida, en la base y no en el disco del contenedor, que corre sin permisos de escritura.
CREATE TABLE fotos_mascotas (
    mascota_id BIGINT PRIMARY KEY REFERENCES mascotas (id),
    tipo_contenido VARCHAR(20) NOT NULL,
    datos BYTEA NOT NULL,
    CONSTRAINT fotos_mascotas_tipo_valido CHECK (tipo_contenido IN ('image/jpeg', 'image/png', 'image/webp'))
);
