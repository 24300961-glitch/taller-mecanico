-- Esquema base del sistema existente. En bases ya pobladas Flyway crea baseline=1 y no ejecuta este archivo.
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(190) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    recovery_phone VARCHAR(20),
    recovery_phone_verified BIT(1) NOT NULL,
    rol_id BIGINT,
    enabled BIT(1) NOT NULL,
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(190),
    updated_by VARCHAR(190),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    clave VARCHAR(60) NOT NULL,
    asignable BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(190),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_clave UNIQUE (clave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users ADD CONSTRAINT fk_users_rol FOREIGN KEY (rol_id) REFERENCES roles (id);

CREATE TABLE rol_permisos (
    rol_id BIGINT NOT NULL,
    permiso VARCHAR(40) NOT NULL,
    PRIMARY KEY (rol_id, permiso),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE talleres (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    activo BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    updated_by VARCHAR(190),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clientes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    nombre_normalizado VARCHAR(210) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    telefono_normalizado VARCHAR(20) NOT NULL,
    telefono_alterno VARCHAR(20),
    telefono_alterno_normalizado VARCHAR(20),
    email VARCHAR(190),
    rfc VARCHAR(13),
    direccion VARCHAR(250),
    activo BIT(1) NOT NULL,
    motivo_desactivacion VARCHAR(250),
    desactivado_en DATETIME(6),
    desactivado_por VARCHAR(190),
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(190),
    updated_at DATETIME(6),
    updated_by VARCHAR(190),
    PRIMARY KEY (id),
    INDEX idx_clientes_nombre_norm (nombre_normalizado),
    INDEX idx_clientes_tel (telefono_normalizado),
    INDEX idx_clientes_tel_alt (telefono_alterno_normalizado),
    INDEX idx_clientes_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehiculos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    marca VARCHAR(40) NOT NULL,
    modelo VARCHAR(60) NOT NULL,
    anio INT NOT NULL,
    placa VARCHAR(15) NOT NULL,
    vin VARCHAR(17),
    color VARCHAR(30),
    activo BIT(1) NOT NULL,
    motivo_desactivacion VARCHAR(250),
    desactivado_en DATETIME(6),
    desactivado_por VARCHAR(190),
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(190),
    updated_at DATETIME(6),
    updated_by VARCHAR(190),
    PRIMARY KEY (id),
    CONSTRAINT uk_vehiculos_placa UNIQUE (placa),
    CONSTRAINT uk_vehiculos_vin UNIQUE (vin),
    CONSTRAINT fk_vehiculos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE citas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_cliente VARCHAR(150) NOT NULL,
    telefono VARCHAR(100) NOT NULL,
    email VARCHAR(190),
    vehiculo VARCHAR(100) NOT NULL,
    placa VARCHAR(100),
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    servicio VARCHAR(250) NOT NULL,
    notas VARCHAR(1000),
    estado VARCHAR(25) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(60) NOT NULL,
    performed_by VARCHAR(190),
    target_user VARCHAR(190),
    detail VARCHAR(2000),
    remote_ip VARCHAR(45),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_performedBy (performed_by),
    INDEX idx_audit_createdAt (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
