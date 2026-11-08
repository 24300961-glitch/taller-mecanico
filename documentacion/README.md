# Documentación del proyecto — Taller Mecánico

Documentación viva del sistema. Se actualiza junto con el código, no después.

## Contenido

| Carpeta | Qué contiene | Cuándo usarla |
| --- | --- | --- |
| `backend/` | Contrato del documentador backend, mejores prácticas Spring Boot y mapa real del código | Tareas de API, datos o seguridad |
| `frontend/` | Contrato del documentador frontend, mejores prácticas React y mapa de páginas | Tareas de UI o integración con la API |
| `despliegue/` | Publicación en 3 capas: frontend, API y base de datos | Antes de publicar |
| `auditoria/` | Informes de dependencias y vulnerabilidades | Antes de cada entrega o publicación |

## Reglas

1. La documentación describe **lo que existe**, no lo que debería existir.
2. Una tabla por entidad; un archivo por módulo.
3. Español, frases cortas, UTF-8 sin BOM.
4. Si el código cambia, se actualiza el documento en el mismo cambio.
5. No se crean carpetas vacías.
6. Nada de datos inventados: lo que no se puede verificar se marca como pendiente.

## Relación con `Spec/`

| `Spec/` | `documentacion/` |
| --- | --- |
| Contratos de los agentes (orquestador, QA, skills) | Contratos de los documentadores y guías de calidad |
| Reglas de ejecución | Contenido documental del proyecto |

El catálogo cerrado del orquestador está en `Spec/arquitectura/orquestador.md`, sección 12.
