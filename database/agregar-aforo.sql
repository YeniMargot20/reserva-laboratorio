   -- Agrega el número de asistentes a cada reserva (control de aforo).
   -- Ejecutar una sola vez en MySQL, después de estructura.sql.
   -- Las reservas existentes quedan con 1 asistente.

   USE reserva_laboratorio_db;

   ALTER TABLE reservas
     ADD COLUMN numero_asistentes INT NOT NULL DEFAULT 1;