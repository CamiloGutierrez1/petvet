-- Veterinarios con nombres y especialidades del catálogo para la sustentación.
-- Solo renombra los registros de V1 que conservan su nombre genérico, así no pisa cambios hechos a mano.
-- Los dos primeros coinciden con los datos simulados del frontend (frontend/src/mocks/veterinarios.json).
UPDATE veterinarios SET nombre = 'Dra. Paula Castaño', especialidad = 'Medicina general'
    WHERE id = 1 AND nombre = 'Veterinario Demo 1';
UPDATE veterinarios SET nombre = 'Dr. Andrés Villamil', especialidad = 'Dermatología'
    WHERE id = 2 AND nombre = 'Veterinario Demo 2';
UPDATE veterinarios SET nombre = 'Dra. Valentina Ríos', especialidad = 'Cirugía'
    WHERE id = 3 AND nombre = 'Veterinario Demo 3';

INSERT INTO veterinarios (nombre, especialidad) VALUES
    ('Dr. Julián Herrera', 'Odontología'),
    ('Dra. Sofía Cárdenas', 'Medicina felina');
