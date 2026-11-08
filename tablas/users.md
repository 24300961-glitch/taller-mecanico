# Tabla: users

Almacena las cuentas de acceso al sistema. Cada usuario tiene un correo único, contraseña hasheada, teléfono de recuperación y un rol asignado. Registra auditoría básica de creación, última sesión y estado de activación.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del usuario |
| email | VARCHAR(190) | No | Correo electrónico del usuario (único en el sistema) |
| full_name | VARCHAR(100) | No | Nombre completo del usuario |
| password_hash | VARCHAR(100) | No | Hash de la contraseña (BCrypt u otro algoritmo configurado) |
| recovery_phone | VARCHAR(20) | Sí | Teléfono de recuperación de cuenta |
| recovery_phone_verified | BIT(1) | No | Indica si el teléfono de recuperación fue verificado (0=No, 1=Sí) |
| rol_id | BIGINT | Sí | Referencia al rol asignado al usuario |
| enabled | BIT(1) | No | Estado de la cuenta (0=Deshabilitada, 1=Habilitada) |
| last_login_at | DATETIME(6) | Sí | Fecha y hora del último inicio de sesión exitoso |
| created_at | DATETIME(6) | No | Fecha y hora de creación del registro |
| updated_at | DATETIME(6) | Sí | Fecha y hora de la última actualización |
| created_by | VARCHAR(190) | Sí | Email del usuario que creó el registro |
| updated_by | VARCHAR(190) | Sí | Email del usuario que realizó la última modificación |

## Relaciones

- FK: `rol_id` → `roles(id)` (`fk_users_rol`) — El rol asignado al usuario.

## Índices únicos

- `uk_users_email` — `email`: Garantiza que no existan dos cuentas con el mismo correo.

## Notas

- El campo `email` también sirve como identificador de sesión (campo `username` en Spring Security).
- `password_hash` almacena la contraseña procesada; nunca la contraseña en texto plano.
- `rol_id` es nullable para permitir que el usuario exista sin rol asignado temporalmente.
- Fuente: `V1__esquema_inicial.sql`