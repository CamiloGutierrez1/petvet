-- Historial de ejemplo para la demo y la sustentación.
--
-- Solo se carga cuando Flyway incluye classpath:db/demo (docker-compose.demo.yml lo hace; en el
-- Compose principal se activa con PETVET_CITAS_MIGRACIONES). Las mascotas 1, 2 y 3 son Tobby,
-- Misha y Kiwi, las mismas de frontend/src/mocks/mascotas.json y scripts/mascotas_demo.py.
--
-- Las fechas se calculan al aplicar el archivo para que las vacunas queden al día: los refuerzos
-- vencen en uno a tres meses en lugar de aparecer vencidos desde hace años.
--
-- Es una migración repetible: Flyway la vuelve a ejecutar si cambia este archivo. Los ids altos no
-- chocan con los que asigna la base y ON CONFLICT DO NOTHING evita duplicados al repetirla. Si una
-- cita de ejemplo choca con una cita real, se omite junto con su historial.

INSERT INTO citas (id, mascota_id, veterinario_id, fecha_hora, tipo_servicio, estado, duracion_minutos)
VALUES
    -- Tobby: polivalente hace 11 meses, rabia hace 10 y una consulta por otitis hace 15 días.
    (900001, 1, 1, CURRENT_DATE - INTERVAL '11 months' + INTERVAL '9 hours', 'VACUNACION', 'COMPLETADA', 30),
    (900002, 1, 1, CURRENT_DATE - INTERVAL '10 months' + INTERVAL '10 hours 30 minutes', 'VACUNACION', 'COMPLETADA', 30),
    (900003, 1, 1, CURRENT_DATE - INTERVAL '15 days' + INTERVAL '10 hours', 'CONSULTA_GENERAL', 'COMPLETADA', 30),
    -- Misha: triple felina y rabia hace 9 meses, y esterilización con la cirujana hace 7 meses.
    (900004, 2, 5, CURRENT_DATE - INTERVAL '9 months' + INTERVAL '15 hours', 'VACUNACION', 'COMPLETADA', 30),
    (900006, 2, 3, CURRENT_DATE - INTERVAL '7 months' + INTERVAL '8 hours', 'ESPECIALISTA', 'COMPLETADA', 30),
    -- Kiwi: revisión general hace 2 meses. Las aves no tienen esquema de vacunas en la demo.
    (900005, 3, 1, CURRENT_DATE - INTERVAL '2 months' + INTERVAL '11 hours', 'CONSULTA_GENERAL', 'COMPLETADA', 30)
ON CONFLICT DO NOTHING;

INSERT INTO registros_historial (id, cita_id, mascota_id, veterinario_id, fecha, tipo_consulta,
                                 diagnostico, observaciones, proximo_control)
SELECT v.id, v.cita_id, c.mascota_id, c.veterinario_id, c.fecha_hora + INTERVAL '25 minutes',
       v.tipo_consulta, v.diagnostico, v.observaciones, v.proximo_control
FROM (VALUES
    (900001, 900001, 'GENERAL', 'Control anual: paciente sano, peso estable de 31,5 kg.',
        'Se aplica la polivalente. Queda pendiente el refuerzo de rabia.',
        CAST(CURRENT_DATE - INTERVAL '10 months' AS date)),
    (900002, 900002, 'GENERAL', 'Vacunación antirrábica sin reacciones adversas.',
        'Reposo el resto del día.', CAST(NULL AS date)),
    (900003, 900003, 'GENERAL', 'Otitis leve en oído derecho.',
        'Limpiar el oído antes de cada aplicación.', CAST(CURRENT_DATE + INTERVAL '6 days' AS date)),
    (900004, 900004, 'GENERAL', 'Control anual: buen estado general, peso de 4,2 kg.',
        'Mantener la desparasitación cada tres meses.', CAST(NULL AS date)),
    (900006, 900006, 'ESPECIALISTA', 'Esterilización programada: paciente apta, procedimiento sin complicaciones.',
        'Collar isabelino y reposo por 10 días. Se retiran los puntos en el control.',
        CAST(CURRENT_DATE - INTERVAL '7 months' + INTERVAL '10 days' AS date)),
    (900005, 900005, 'GENERAL', 'Revisión general: plumaje, pico y uñas en buen estado.',
        'Mantener la dieta de semillas con fruta fresca.', CAST(NULL AS date))
) AS v (id, cita_id, tipo_consulta, diagnostico, observaciones, proximo_control)
JOIN citas c ON c.id = v.cita_id AND c.estado = 'COMPLETADA'
ON CONFLICT DO NOTHING;

INSERT INTO historial_vacunas (historial_id, posicion, valor)
SELECT v.historial_id, v.posicion, v.valor
FROM (VALUES
    (900001, 0, 'Polivalente'),
    (900002, 0, 'Rabia'),
    (900004, 0, 'Triple felina'),
    (900004, 1, 'Rabia')
) AS v (historial_id, posicion, valor)
JOIN registros_historial r ON r.id = v.historial_id
ON CONFLICT DO NOTHING;

INSERT INTO historial_tratamientos (historial_id, posicion, valor)
SELECT v.historial_id, v.posicion, v.valor
FROM (VALUES
    (900001, 0, 'Desparasitación interna'),
    (900003, 0, 'Gotas óticas cada 12 horas por 7 días'),
    (900004, 0, 'Desparasitación interna'),
    (900006, 0, 'Meloxicam oral cada 24 horas por 3 días')
) AS v (historial_id, posicion, valor)
JOIN registros_historial r ON r.id = v.historial_id
ON CONFLICT DO NOTHING;

INSERT INTO historial_examenes (historial_id, posicion, valor)
SELECT v.historial_id, v.posicion, v.valor
FROM (VALUES
    (900003, 0, 'Otoscopia'),
    (900004, 0, 'Hemograma'),
    (900005, 0, 'Examen físico general')
) AS v (historial_id, posicion, valor)
JOIN registros_historial r ON r.id = v.historial_id
ON CONFLICT DO NOTHING;

INSERT INTO historial_cirugias (historial_id, posicion, valor)
SELECT v.historial_id, v.posicion, v.valor
FROM (VALUES
    (900006, 0, 'Esterilización')
) AS v (historial_id, posicion, valor)
JOIN registros_historial r ON r.id = v.historial_id
ON CONFLICT DO NOTHING;
