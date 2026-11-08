# Tabla: audit_logs

Registro de auditoría inmutable de las acciones relevantes realizadas en el sistema. Captura quién hizo qué, sobre qué entidad y desde qué dirección IP, para fines de trazabilidad y seguridad.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del evento de auditoría |
| entity_type | VARCHAR(80) | No | Tipo de entidad afectada (p. ej. `USER`, `CLIENTE`, `VEHICULO`) |
| entity_id | BIGINT | Sí | ID de la entidad afectada; puede ser `NULL` para acciones globales (p. ej. login fallido) |
| action | VARCHAR(60) | No | Acción ejecutada (p. ej. `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `PASSWORD_RESET`) |
| performed_by | VARCHAR(190) | Sí | Email del usuario que ejecutó la acción; `NULL` para acciones anónimas |
| target_user | VARCHAR(190) | Sí | Email del usuario objetivo de la acción (p. ej. al cambiar el rol de otro usuario) |
| detail | VARCHAR(2000) | Sí | Detalle adicional del evento en texto libre o JSON |
| remote_ip | VARCHAR(45) | Sí | Dirección IP del cliente (soporte IPv4 e IPv6) |
| created_at | DATETIME(6) | No | Fecha y hora exacta del evento |

## Relaciones

- Sin claves foráneas explícitas. Las referencias a entidades se guardan como texto (`entity_type`/`entity_id`) para que los registros de auditoría sobrevivan a eliminaciones lógicas o físicas.

## Índices únicos

- No hay índices únicos.

## Índices de búsqueda

- `idx_audit_entity` — `(entity_type, entity_id)`: Facilita la consulta del historial de una entidad específica.
- `idx_audit_action` — `action`: Filtra por tipo de acción.
- `idx_audit_performedBy` — `performed_by`: Filtra por usuario ejecutor.
- `idx_audit_createdAt` — `created_at`: Filtra por rango de fechas.

## Notas

- Esta tabla es de solo escritura desde la perspectiva del negocio; los registros no deben modificarse ni eliminarse.
- El campo `remote_ip` acepta hasta 45 caracteres para soportar direcciones IPv6 completas (p. ej. `2001:0db8:85a3:0000:0000:8a2e:0370:7334`).
- Fuente: `V1__esquema_inicial.sql`