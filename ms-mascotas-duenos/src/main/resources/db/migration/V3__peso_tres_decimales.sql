-- Tres decimales para pesar en gramos: un periquito de 35 g pesa 0.035 kg, que NUMERIC(6, 2) redondeaba a 0.04.
-- Siguen cabiendo 4 enteros. Los datos de ejemplo quedan como 31.500, 4.200 y 0.040, y la API los responde sin ceros sobrantes.
ALTER TABLE mascotas ALTER COLUMN peso TYPE NUMERIC(7, 3);
