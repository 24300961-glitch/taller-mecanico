-- Extiende el esquema base sin eliminar filas existentes.
-- Relaciones nullable permiten que vehículos/clientes anteriores sigan disponibles para asignación manual.

ALTER TABLE talleres
    ADD COLUMN rfc VARCHAR(13) NULL,
    ADD COLUMN email VARCHAR(190) NULL,
    ADD COLUMN telefono VARCHAR(20) NULL,
    ADD COLUMN foto_path VARCHAR(512) NULL,
    ADD COLUMN calle VARCHAR(160) NULL,
    ADD COLUMN numero VARCHAR(40) NULL,
    ADD COLUMN colonia VARCHAR(120) NULL,
    ADD COLUMN codigo_postal VARCHAR(5) NULL,
    ADD COLUMN municipio VARCHAR(120) NULL,
    ADD COLUMN estado VARCHAR(120) NULL;

CREATE UNIQUE INDEX uk_talleres_rfc ON talleres (rfc);
CREATE INDEX idx_talleres_nombre_cp ON talleres (nombre, codigo_postal);

CREATE TABLE taller_cliente (
    cliente_id BIGINT NOT NULL,
    taller_id BIGINT NOT NULL,
    PRIMARY KEY (cliente_id, taller_id),
    CONSTRAINT fk_taller_cliente_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_taller_cliente_taller FOREIGN KEY (taller_id) REFERENCES talleres (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE vehiculos ADD COLUMN taller_id BIGINT NULL;
CREATE INDEX idx_vehiculos_taller ON vehiculos (taller_id);
ALTER TABLE vehiculos ADD CONSTRAINT fk_vehiculos_taller FOREIGN KEY (taller_id) REFERENCES talleres (id);

CREATE TABLE catalogo_codigos_postales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo_postal VARCHAR(5) NOT NULL,
    colonia VARCHAR(160) NOT NULL,
    municipio VARCHAR(120) NOT NULL,
    estado VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_sep_codigo_postal (codigo_postal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
