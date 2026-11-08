-- V1 creó recovery_phone_verified como BIT(1) NOT NULL sin valor por defecto.
-- La entidad UserAccount no mapea esa columna, así que el INSERT del administrador
-- inicial falla en MariaDB con sql_mode estricto:
--   Field 'recovery_phone_verified' doesn't have a default value
-- Se agrega el valor por defecto para que cualquier inserción que omita la columna funcione.
ALTER TABLE users MODIFY COLUMN recovery_phone_verified BIT(1) NOT NULL DEFAULT b'0';
