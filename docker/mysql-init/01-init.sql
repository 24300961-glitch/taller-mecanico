-- ============================================================
-- SQL DE INICIALIZACION - MySQL 8.4
-- Se ejecuta AUTOMATICAMENTE solo la PRIMERA vez que se crea
-- el volumen mysql_data. Si ya existe la BD, no hace nada.
-- ============================================================

-- Asegurar que la base de datos exista (por si acaso)
CREATE DATABASE IF NOT EXISTS taller_mecanico
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- NOTA: el usuario 'taller' y su contrasena los crea automaticamente
-- la imagen oficial de MySQL con MYSQL_USER / MYSQL_PASSWORD de compose.
-- No crear el usuario aqui con otra contrasena: provocaria que la app
-- (que usa DB_PASSWORD de .env) no pueda autenticarse.
GRANT ALL PRIVILEGES ON taller_mecanico.* TO 'taller'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;

-- ============================================================
-- NOTA:
-- Flyway aplica las migraciones versionadas de
-- src/main/resources/db/migration al iniciar la aplicación.
-- En Compose, Hibernate valida el esquema con ddl-auto=validate.
-- ============================================================
