-- Añade los campos vehiculares ya definidos por el dominio sin modificar filas existentes.
-- Las comprobaciones hacen la migración compatible con instalaciones donde estas columnas
-- se hayan agregado manualmente antes de Flyway.

SET @ddl_version = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'vehiculos' AND column_name = 'version') = 0,
    'ALTER TABLE vehiculos ADD COLUMN version VARCHAR(30) NULL',
    'SELECT 1'
);
PREPARE stmt_version FROM @ddl_version;
EXECUTE stmt_version;
DEALLOCATE PREPARE stmt_version;

SET @ddl_kilometraje = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'vehiculos' AND column_name = 'kilometraje') = 0,
    'ALTER TABLE vehiculos ADD COLUMN kilometraje INT NULL',
    'SELECT 1'
);
PREPARE stmt_kilometraje FROM @ddl_kilometraje;
EXECUTE stmt_kilometraje;
DEALLOCATE PREPARE stmt_kilometraje;

SET @ddl_fotos = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'vehiculos' AND column_name = 'fotos') = 0,
    'ALTER TABLE vehiculos ADD COLUMN fotos VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt_fotos FROM @ddl_fotos;
EXECUTE stmt_fotos;
DEALLOCATE PREPARE stmt_fotos;
