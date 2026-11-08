# Tabla: taller_cliente

Tabla pivote de la relación N:M entre talleres y clientes. Permite que un cliente esté vinculado a múltiples talleres y que un taller tenga múltiples clientes registrados.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| cliente_id | BIGINT PK | No | Referencia al cliente; parte de la clave primaria compuesta |
| taller_id | BIGINT PK | No | Referencia al taller; parte de la clave primaria compuesta |

## Relaciones

- FK: `cliente_id` → `clientes(id)` (`fk_taller_cliente_cliente`) — El cliente vinculado al taller.
- FK: `taller_id` → `talleres(id)` (`fk_taller_cliente_taller`) — El taller al que pertenece el cliente.

## Índices únicos

- Clave primaria compuesta `(cliente_id, taller_id)`: Garantiza que la misma combinación cliente-taller no se registre duplicada.

## Notas

- Esta tabla no tiene columnas adicionales (fecha de alta, estado, etc.); es una tabla de asociación pura.
- Si se requiere auditar cuándo se vinculó un cliente a un taller, se debería agregar `created_at` y `created_by` en una migración futura.
- Fuente: `V2__talleres_clientes_vehiculos_sepomex.sql`
