-- Ejecutar solo si la columna numero_asistentes no existe.
-- Comprobar primero con:
-- SHOW COLUMNS FROM reserva_laboratorio_db.reservas
-- LIKE 'numero_asistentes';

ALTER TABLE reserva_laboratorio_db.reservas
ADD COLUMN numero_asistentes INT NULL;