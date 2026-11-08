# Tabla: clientes

Almacena el perfil de cada cliente registrado en el sistema. Incluye datos personales, de contacto y campos normalizados para búsqueda eficiente por nombre o teléfono. Soporta desactivación lógica con trazabilidad.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del cliente |
| nombre | VARCHAR(80) | No | Nombre(s) del cliente |
| apellidos | VARCHAR(120) | No | Apellidos del cliente |
| nombre_normalizado | VARCHAR(210) | No | Nombre completo normalizado (sin acentos, minúsculas) para búsqueda case-insensitive |
| telefono | VARCHAR(20) | No | Teléfono principal de contacto (formato original) |
| telefono_normalizado | VARCHAR(20) | No | Teléfono principal normalizado (solo dígitos) para búsquedas |
| telefono_alterno | VARCHAR(20) | Sí | Teléfono alterno de contacto (formato original) |
| telefono_alterno_normalizado | VARCHAR(20) | Sí | Teléfono alterno normalizado (solo dígitos) para búsquedas |
| email | VARCHAR(190) | Sí | Correo electrónico del cliente |
| rfc | VARCHAR(13) | Sí | RFC del cliente (para facturación) |
| direccion | VARCHAR(250) | Sí | Dirección libre del cliente |
| activo | BIT(1) | No | Estado del cliente (0=Inactivo, 1=Activo) |
| motivo_desactivacion | VARCHAR(250) | Sí | Razón por la que el cliente fue desactivado |
| desactivado_en | DATETIME(6) | Sí | Fecha y hora en que el cliente fue desactivado |
| desactivado_por | VARCHAR(190) | Sí | Email del usuario que desactivó al cliente |
| created_at | DATETIME(6) | No | Fecha y hora de creación del registro |
| created_by | VARCHAR(190) | Sí | Email del usuario que creó el registro |
| updated_at | DATETIME(6) | Sí | Fecha y hora de la última actualización |
| updated_by | VARCHAR(190) | Sí | Email del usuario que realizó la última modificación |

## Relaciones

- Referenciada por: `vehiculos(cliente_id)` y `taller_cliente(cliente_id)`.

## Índices únicos

- No hay índices únicos (el RFC no tiene restricción única a nivel de cliente).

## Índices de búsqueda

- `idx_clientes_nombre_norm` — `nombre_normalizado`: Búsqueda rápida por nombre completo normalizado.
- `idx_clientes_tel` — `telefono_normalizado`: Búsqueda por teléfono principal.
- `idx_clientes_tel_alt` — `telefono_alterno_normalizado`: Búsqueda por teléfono alterno.
- `idx_clientes_email` — `email`: Búsqueda por correo electrónico.

## Notas

- Los campos `*_normalizado` son mantenidos por la aplicación en cada inserción/actualización; no tienen triggers de BD.
- La desactivación es lógica: el registro permanece en la BD con `activo = 0` y los campos de trazabilidad completados.
- `nombre_normalizado` es la concatenación de `nombre` + `apellidos` normalizada (sin acentos, en minúsculas).
- Fuente: `V1__esquema_inicial.sql`