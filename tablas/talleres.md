# Tabla: talleres

Representa cada taller mecánico registrado en la plataforma. Almacena datos de identificación, contacto y dirección postal completa. Es la entidad central del sistema multitaller.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del taller |
| nombre | VARCHAR(120) | No | Nombre comercial del taller |
| activo | BIT(1) | No | Estado del taller (0=Inactivo, 1=Activo) |
| created_at | DATETIME(6) | No | Fecha y hora de creación del registro |
| updated_at | DATETIME(6) | Sí | Fecha y hora de la última actualización |
| updated_by | VARCHAR(190) | Sí | Email del usuario que realizó la última modificación |
| rfc | VARCHAR(13) | Sí | RFC del taller (único; agregado en V2) |
| email | VARCHAR(190) | Sí | Correo electrónico de contacto del taller (agregado en V2) |
| telefono | VARCHAR(20) | Sí | Teléfono de contacto del taller (agregado en V2) |
| foto_path | VARCHAR(512) | Sí | Ruta relativa o URL de la fotografía/logo del taller (agregado en V2) |
| calle | VARCHAR(160) | Sí | Nombre de la calle de la dirección (agregado en V2) |
| numero | VARCHAR(40) | Sí | Número exterior/interior de la dirección (agregado en V2) |
| colonia | VARCHAR(120) | Sí | Colonia de la dirección (agregado en V2) |
| codigo_postal | VARCHAR(5) | Sí | Código postal de 5 dígitos (agregado en V2) |
| municipio | VARCHAR(120) | Sí | Municipio o delegación (agregado en V2) |
| estado | VARCHAR(120) | Sí | Estado de la república (agregado en V2) |

## Relaciones

- Referenciada por: `taller_cliente(taller_id)` y `vehiculos(taller_id)`.

## Índices únicos

- `uk_talleres_rfc` — `rfc`: Garantiza que no existan dos talleres registrados con el mismo RFC (agregado en V2).

## Índices de búsqueda

- `idx_talleres_nombre_cp` — `(nombre, codigo_postal)`: Facilita búsquedas por nombre y zona postal (agregado en V2).

## Notas

- Las columnas de dirección y contacto son todas nullable para mantener compatibilidad con registros existentes antes de la migración V2.
- `foto_path` puede almacenar una ruta local relativa al servidor o una URL completa dependiendo de la configuración del almacenamiento.
- El `codigo_postal` es VARCHAR(5) para preservar ceros a la izquierda.
- Fuentes: `V1__esquema_inicial.sql` (columnas base) + `V2__talleres_clientes_vehiculos_sepomex.sql` (columnas de contacto y dirección).