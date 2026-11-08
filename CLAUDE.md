# Taller Mecánico — Claude Code

@AGENTS.md

## Agentes

Los agentes están en `.claude/agents/`. Usa el que corresponda:

- `orquestador` — entrada principal.
- `qa` — pruebas.
- `documentador-backend` — documentación del backend.
- `documentador-frontend` — documentación del frontend.
- `auditor-dependencias` — librerías y vulnerabilidades.

Cada uno lee su contrato completo antes de actuar. El catálogo cerrado está en `Spec/arquitectura/orquestador.md`, sección 12.
