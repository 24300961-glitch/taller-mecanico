# Taller Mecánico — Guía del repositorio

Este archivo es la **entrada común** para cualquier herramienta de IA (OpenCode, Claude Code, Codex, Cursor, Copilot, Gemini CLI, Windsurf, Cline, Roo, Aider). Si tu herramienta no lee `AGENTS.md`, usa el archivo puente que corresponda; todos llevan aquí.

## 1. Qué es el proyecto

Sistema de gestión para un taller mecánico:

- **Backend:** Java 25 + Spring Boot 3.5, API REST, sesión con cookie y CSRF.
- **Frontend:** React 18 + Vite, SPA que consume `/api/*` en el mismo origen.
- **Base de datos:** MySQL 8 con migraciones Flyway.

## 2. Estructura

```
backend/            API Spring Boot y migraciones Flyway
frontend/           SPA React + Vite
Spec/               contratos de agentes y skills
  arquitectura/orquestador.md   contrato principal (catálogo en la sección 12)
  SKILL/Backend/skills.md       guía técnica Spring Boot
  SKILL/Frontend/frontend.md    guía técnica React
documentacion/      documentación viva del proyecto
  backend/          documentador, mejores prácticas y estructura
  frontend/         documentador, mejores prácticas y estructura
  despliegue/       publicación en 3 capas
  auditoria/        informes de dependencias
tablas/             una ficha por tabla
diagramas/          arquitectura y flujos
```

## 3. Comandos

```powershell
# Backend (requiere JDK 25)
$env:JAVA_HOME="C:\Users\ac765\.jdks\openjdk-25.0.1"
mvn -DskipTests compile
mvn -DskipTests package

# Frontend
npm --prefix frontend install
npm --prefix frontend run dev
npm --prefix frontend run build

# Todo junto
docker compose up --build
```

## 4. Convenciones de código

- Java: 4 espacios, paquetes en minúsculas, clases `PascalCase`, métodos y variables `camelCase`.
- React: componentes `PascalCase`, archivos por página en `src/pages/`, compartidos en `src/components/`.
- Textos visibles al usuario: **español**.
- Validar los DTO en el borde de la API y usar paquetes por funcionalidad (`clientes/`, `vehiculos/`, `taller/`...).
- Migraciones Flyway secuenciales: `V7__descripcion.sql`. Nunca editar una migración ya aplicada.
- No hay formateador ni linter global; imita el código cercano.

## 5. Agentes disponibles

| Agente | Contrato | Cuándo usarlo |
| --- | --- | --- |
| `orquestador` | `Spec/arquitectura/orquestador.md` | Entrada principal: analiza, diseña, implementa y cierra |
| `qa` | `Spec/Agentes/qa.md` | Crear y ejecutar pruebas sin tocar la BD real |
| `documentador-backend` | `documentacion/backend/documentador-backend.md` | Documentar clases, endpoints y validaciones |
| `documentador-frontend` | `documentacion/frontend/documentador-frontend.md` | Documentar páginas, componentes y hooks |
| `auditor-dependencias` | `.opencode/agents/auditor-dependencias.md` | Revisar y actualizar librerías |

**Catálogo cerrado y matriz de activación:** `Spec/arquitectura/orquestador.md`, sección 12.
**Guías de calidad:** `documentacion/backend/mejores-practicas-spring-boot.md` y `documentacion/frontend/mejores-practicas-react.md`.

## 6. Reglas no negociables

1. No inventar archivos, pruebas, comandos ni resultados.
2. No ampliar el alcance sin autorización.
3. No crear datos de prueba en la base de datos persistente.
4. No tocar `.env`, credenciales ni secretos.
5. No ejecutar `git push` ni operaciones destructivas sobre la BD.
6. Toda entrega cierra con **tabla de fases** y **resumen ejecutivo**.
7. Documentar en español, UTF-8 sin BOM.

## 7. Pruebas y verificación

- Backend: `mvn test` (JUnit 5 + Mockito). Sin tests actualmente en `backend/src/test/`.
- Frontend: `npm run build` para validar que compila.
- Nunca apuntar pruebas a la base de datos real; usar H2 en memoria o una BD desechable.
- Para cambios de UI: `npm run build` y revisión visual.

## 8. Seguridad

- Contraseñas con BCrypt; nunca en texto plano ni en logs.
- Sesión con cookie, CSRF activo y autorización por rol.
- Esquema solo por Flyway.
- No registrar contraseñas, tokens de recuperación ni datos personales.
- Variables de entorno documentadas con marcadores en `.env.example`; los valores reales van en `.env` (ignorado por Git).
