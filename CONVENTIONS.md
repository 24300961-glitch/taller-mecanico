# Convenciones del proyecto — Taller Mecánico

Lee y aplica `AGENTS.md` en la raíz del repositorio. Este archivo existe para herramientas que buscan `CONVENTIONS.md` (por ejemplo Aider).

- No inventar archivos, pruebas ni resultados.
- No ampliar el alcance sin autorización.
- No crear datos de prueba en la base de datos persistente.
- No tocar `.env`, credenciales ni secretos.
- Java: 4 espacios; clases `PascalCase`; métodos `camelCase`.
- React: componentes `PascalCase`; llamadas `fetch` solo en `frontend/src/api.js`.
- Textos al usuario en español; documentación en UTF-8 sin BOM.
- Migraciones Flyway secuenciales; nunca editar una aplicada.

Catálogo de agentes: `Spec/arquitectura/orquestador.md`, sección 12.
