-- Decisión 28: administración de roles con suspensión (soft-delete) y ficha
-- ampliada de clientes (apellidos divididos, teléfonos, contacto y domicilio).
-- Todo aditivo: no se borra ni se renombra ninguna columna existente.

-- 1) Roles: bandera de suspensión. Los suspendidos no se ofrecen para asignar.
-- (En tres pasos porque BIT + DEFAULT directo no es portable.)
ALTER TABLE roles
    ADD COLUMN activo BIT(1) NULL;
UPDATE roles SET activo = 1 WHERE activo IS NULL;
ALTER TABLE roles
    MODIFY COLUMN activo BIT(1) NOT NULL;

-- 2) Clientes: apellidos divididos, teléfonos adicionales, contacto alternativo
-- y domicilio dividido. Se conservan `apellidos` y `direccion` heredados.
ALTER TABLE clientes
    ADD COLUMN apellido_paterno VARCHAR(80) NULL,
    ADD COLUMN apellido_materno VARCHAR(80) NULL,
    ADD COLUMN telefono_personal VARCHAR(20) NULL,
    ADD COLUMN telefono_personal_normalizado VARCHAR(20) NULL,
    ADD COLUMN telefono_trabajo VARCHAR(20) NULL,
    ADD COLUMN telefono_trabajo_normalizado VARCHAR(20) NULL,
    ADD COLUMN nombre_contacto VARCHAR(160) NULL,
    ADD COLUMN telefono_contacto VARCHAR(20) NULL,
    ADD COLUMN telefono_contacto_normalizado VARCHAR(20) NULL,
    ADD COLUMN calle VARCHAR(160) NULL,
    ADD COLUMN numero VARCHAR(40) NULL,
    ADD COLUMN colonia VARCHAR(120) NULL,
    ADD COLUMN codigo_postal VARCHAR(5) NULL,
    ADD COLUMN municipio VARCHAR(120) NULL,
    ADD COLUMN estado VARCHAR(120) NULL;

-- Relleno inicial: el apellido heredado pasa a paterno; el resto queda manual.
UPDATE clientes SET apellido_paterno = apellidos WHERE apellido_paterno IS NULL;

CREATE INDEX idx_clientes_tel_per ON clientes (telefono_personal_normalizado);
CREATE INDEX idx_clientes_tel_tra ON clientes (telefono_trabajo_normalizado);
CREATE INDEX idx_clientes_tel_con ON clientes (telefono_contacto_normalizado);
