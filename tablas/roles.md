# Tabla: roles

Catálogo de roles del sistema de control de acceso basado en roles (RBAC). Cada rol agrupa un conjunto de permisos y puede ser asignado a uno o más usuarios.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del rol |
| nombre | VARCHAR(60) | No | Nombre descriptivo del rol (p. ej. "Administrador", "Mecánico") |
| clave | VARCHAR(60) | No | Clave técnica única del rol (p. ej. `ROLE_ADMIN`, `ROLE_MECANICO`) |
| asignable | BIT(1) | No | Indica si el rol puede asignarse manualmente desde la UI (0=No, 1=Sí) |
| activo | BIT(1) | No | Indica si el rol está habilitado (agregado en V5) |
| created_at | DATETIME(6) | No | Fecha y hora de creación del registro |
| created_by | VARCHAR(190) | Sí | Email del usuario que creó el rol |

## Relaciones

- Ninguna FK hacia otras tablas (es tabla padre).
- Referenciada por: `users(rol_id)` y `rol_permisos(rol_id)`.

## Índices únicos

- `uk_roles_clave` — `clave`: Garantiza que no existan dos roles con la misma clave técnica.

## Notas

- Los roles marcados con `asignable = 0` son roles del sistema que no deben asignarse manualmente (p. ej. rol raíz o de servicio).
- La clave se usa en Spring Security para evaluación de autorización.
- `app.seguridad.roles-iniciales` configura los ocho roles solicitados; sus claves se derivan del nombre como `ROLE_<CLAVE>`.
- Fuentes: `V1__esquema_inicial.sql` y `V5__roles_activo_clientes_ficha.sql`.
