# Tabla: vehiculos

Almacena los vehículos registrados en el sistema, asociados a un cliente propietario y opcionalmente al taller donde se atienden. Soporta desactivación lógica con trazabilidad.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del vehículo |
| cliente_id | BIGINT | No | Referencia al cliente propietario del vehículo |
| marca | VARCHAR(40) | No | Marca del vehículo (p. ej. "Toyota", "Ford") |
| modelo | VARCHAR(60) | No | Modelo del vehículo (p. ej. "Corolla", "Ranger") |
| anio | INT | No | Año de fabricación del vehículo |
| placa | VARCHAR(15) | No | Número de placa (único en el sistema) |
| vin | VARCHAR(17) | Sí | Número de identificación vehicular VIN (único si se captura) |
| color | VARCHAR(30) | Sí | Color del vehículo |
| version | VARCHAR(30) | Sí | Versión o submodelo informado |
| kilometraje | INT | Sí | Kilometraje; si se captura, debe ser mayor o igual a cero |
| fotos | VARCHAR(500) | Sí | Rutas de archivos de fotos separadas por coma |
| activo | BIT(1) | No | Estado del vehículo (0=Inactivo, 1=Activo) |
| motivo_desactivacion | VARCHAR(250) | Sí | Razón por la que el vehículo fue desactivado |
| desactivado_en | DATETIME(6) | Sí | Fecha y hora en que el vehículo fue desactivado |
| desactivado_por | VARCHAR(190) | Sí | Email del usuario que desactivó el vehículo |
| created_at | DATETIME(6) | No | Fecha y hora de creación del registro |
| created_by | VARCHAR(190) | Sí | Email del usuario que creó el registro |
| updated_at | DATETIME(6) | Sí | Fecha y hora de la última actualización |
| updated_by | VARCHAR(190) | Sí | Email del usuario que realizó la última modificación |
| taller_id | BIGINT | Sí | Referencia al taller donde se atiende el vehículo (agregado en V2) |

## Relaciones

- FK: `cliente_id` → `clientes(id)` (`fk_vehiculos_cliente`) — Propietario del vehículo.
- FK: `taller_id` → `talleres(id)` (`fk_vehiculos_taller`) — Taller donde se atiende el vehículo (agregado en V2).

## Índices únicos

- `uk_vehiculos_placa` — `placa`: Garantiza que no existan dos vehículos con la misma placa.
- `uk_vehiculos_vin` — `vin`: Garantiza unicidad del VIN cuando se captura.

## Índices de búsqueda

- `idx_vehiculos_taller` — `taller_id`: Facilita la consulta de todos los vehículos de un taller (agregado en V2).

## Notas

- `taller_id` es nullable para mantener compatibilidad con vehículos registrados antes de la migración V2 que aún no tienen taller asignado.
- El `vin` acepta `NULL` porque no todos los vehículos (especialmente los más antiguos) cuentan con este número.
- La desactivación es lógica: el registro permanece en la BD con `activo = 0`.
- Fuentes: `V1__esquema_inicial.sql` (columnas base), `V2__talleres_clientes_vehiculos_sepomex.sql` (`taller_id`) y `V6__campos_completos_vehiculos.sql` (`version`, `kilometraje`, `fotos`).
