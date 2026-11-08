# Instrucciones para GitHub Copilot — Taller Mecánico

Lee y aplica `AGENTS.md` en la raíz del repositorio. Este archivo resume lo indispensable.

## Proyecto

- Backend: Java 25 + Spring Boot 3.5, API REST con sesión y CSRF.
- Frontend: React 18 + Vite, consume `/api/*` en el mismo origen.
- Base de datos: MySQL 8 con Flyway.

## Reglas

1. No inventar archivos, pruebas, comandos ni resultados.
2. No ampliar el alcance sin autorización.
3. No crear datos de prueba en la base de datos persistente.
4. No tocar `.env`, credenciales ni secretos.
5. Java: 4 espacios; clases `PascalCase`; métodos `camelCase`.
6. React: componentes `PascalCase`; una página en `src/pages/`, compartidos en `src/components/`.
7. Textos al usuario en español.
8. Migraciones Flyway secuenciales; nunca editar una ya aplicada.
9. Toda llamada `fetch` va en `frontend/src/api.js`.
10. Toda entrega cierra con tabla de fases y resumen ejecutivo.

## Referencias

- Contrato principal: `Spec/arquitectura/orquestador.md` (catálogo en la sección 12).
- Calidad backend: `documentacion/backend/mejores-practicas-spring-boot.md`.
- Calidad frontend: `documentacion/frontend/mejores-practicas-react.md`.
