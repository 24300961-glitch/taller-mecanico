# Tabla: rol_permisos

Tabla de permisos asignados a cada rol. Implementa el modelo RBAC (Role-Based Access Control) mediante una relación de muchos a muchos entre roles y permisos individuales. Cada fila representa un permiso granular otorgado a un rol específico.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| rol_id | BIGINT PK | No | Referencia al rol al que pertenece el permiso |
| permiso | VARCHAR(40) PK | No | Nombre del permiso (p. ej. `CLIENTE_LEER`, `ORDEN_CREAR`) |

## Relaciones

- FK: `rol_id` → `roles(id)` (`fk_rol_permisos_rol`) — El rol al que se le asigna el permiso.

## Índices únicos

- Clave primaria compuesta `(rol_id, permiso)`: Garantiza que un mismo permiso no se duplique dentro del mismo rol.

## Notas

- No existe una tabla separada de "catálogo de permisos"; los valores de `permiso` son cadenas libres definidas en la lógica de negocio de la aplicación.
- Al cargar un usuario en Spring Security, se combinan su rol y sus permisos para construir el `GrantedAuthority` completo.
- Fuente: `V1__esquema_inicial.sql`