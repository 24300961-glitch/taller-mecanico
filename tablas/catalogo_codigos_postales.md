# Tabla: catalogo_codigos_postales

Catálogo de códigos postales de México basado en datos del SEPOMEX (Servicio Postal Mexicano). Permite autocompletar colonia, municipio y estado a partir de un código postal capturado en formularios de dirección de talleres y clientes.

## Columnas

| Columna | Tipo | Nullable | Descripción |
|---|---|---|---|
| id | BIGINT PK AUTO_INCREMENT | No | Identificador único del registro |
| codigo_postal | VARCHAR(5) | No | Código postal de 5 dígitos |
| colonia | VARCHAR(160) | No | Nombre de la colonia o asentamiento correspondiente al CP |
| municipio | VARCHAR(120) | No | Municipio o alcaldía al que pertenece la colonia |
| estado | VARCHAR(120) | No | Entidad federativa (estado) al que pertenece el municipio |

## Relaciones

- Sin claves foráneas. Es un catálogo de referencia de solo lectura.
- Utilizado como fuente de datos por: `talleres(codigo_postal)` (sin FK explícita; la relación es lógica/aplicativa).

## Índices únicos

- No hay índices únicos. Un mismo código postal puede tener múltiples colonias.

## Índices de búsqueda

- `idx_sep_codigo_postal` — `codigo_postal`: Permite recuperar rápidamente todas las colonias de un CP dado.

## Notas

- La tabla es de solo lectura en producción; se carga inicialmente con el archivo oficial del SEPOMEX y se actualiza periódicamente conforme SEPOMEX publica nuevas versiones.
- Un código postal puede corresponder a varias colonias; por eso la PK es `id` y no `(codigo_postal, colonia)`.
- `codigo_postal` es VARCHAR(5) para preservar el cero inicial de CPs como `01000` (Ciudad de México).
- Fuente: `V2__talleres_clientes_vehiculos_sepomex.sql`
