# Tabla: password_reset_tokens

Almacena los tokens de restablecimiento de contraseña generados cuando un usuario solicita recuperar su acceso. Cada token es de un solo uso y tiene fecha de expiración.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del token |
| token_hash | VARCHAR(64) | No | Hash SHA-256 (u otro) del token enviado al usuario; nunca se almacena el token en texto plano |
| user_id | BIGINT | No | Referencia al usuario al que pertenece el token |
| expires_at | DATETIME(6) | No | Fecha y hora de expiración del token |
| used_at | DATETIME(6) | Sí | Fecha y hora en que el token fue utilizado; `NULL` si aún no se ha usado |

## Relaciones

- FK: `user_id` → `users(id)` (`fk_password_reset_user`) — El usuario propietario del token.

## Índices únicos

- `uk_password_reset_token_hash` — `token_hash`: Garantiza que no existan dos tokens con el mismo hash, y permite búsqueda eficiente durante la validación.

## Notas

- La aplicación debe rechazar tokens con `expires_at < NOW()` o con `used_at IS NOT NULL`.
- Al usar un token se debe actualizar `used_at` para invalidarlo y prevenir reutilización.
- Se recomienda limpiar periódicamente los tokens expirados o ya usados para mantener la tabla pequeña.
- Fuente: `V1__esquema_inicial.sql`