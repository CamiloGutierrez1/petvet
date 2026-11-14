-- Horario de atención de cada veterinario: una franja de inicio a fin, igual todos los días.
-- El valor por defecto es el horario general de la clínica, de 08:00 a 18:00.
ALTER TABLE veterinarios
    ADD COLUMN hora_inicio TIME NOT NULL DEFAULT '08:00',
    ADD COLUMN hora_fin TIME NOT NULL DEFAULT '18:00',
    ADD CONSTRAINT veterinarios_horario_valido CHECK (hora_inicio < hora_fin);

-- Horarios de ejemplo para los veterinarios de V5. La Dra. Paula Castaño (id 1) conserva el horario
-- completo, de 08:00 a 18:00.
UPDATE veterinarios SET hora_inicio = '09:00', hora_fin = '17:00' WHERE id = 2; -- Dr. Andrés Villamil
UPDATE veterinarios SET hora_inicio = '07:00', hora_fin = '13:00' WHERE id = 3; -- Dra. Valentina Ríos
UPDATE veterinarios SET hora_inicio = '13:00', hora_fin = '19:00' WHERE id = 4; -- Dr. Julián Herrera
UPDATE veterinarios SET hora_inicio = '10:00', hora_fin = '16:00' WHERE id = 5; -- Dra. Sofía Cárdenas
