-- Historial de movimientos de vehículos (decisión 25): registra altas, cambios de
-- taller o cliente, correcciones, desactivaciones y reactivaciones con los nombres
-- vigentes al momento del evento. Solo lectura hacia atrás: no hay datos que migrar.

CREATE TABLE historial_vehiculos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehiculo_id BIGINT NOT NULL,
    placa VARCHAR(15) NOT NULL,
    cliente_nombre VARCHAR(200) NOT NULL,
    taller_nombre VARCHAR(120),
    tipo VARCHAR(20) NOT NULL,
    detalle VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(190),
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_historial_vehiculo ON historial_vehiculos (vehiculo_id);
CREATE INDEX idx_historial_cliente ON historial_vehiculos (cliente_nombre);
