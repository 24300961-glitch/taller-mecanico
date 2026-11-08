# Auditoría de dependencias — 2026-11-07

## Resumen

| Métrica | Backend (Maven) | Frontend (npm) |
| --- | --- | --- |
| Dependencias directas revisadas | 10 | 6 |
| Desactualizadas | 0 en la línea 3.5 | 6 |
| Con vulnerabilidades | No evaluado (falta OWASP) | 5 → **4** tras actualizar |
| Actualizadas en esta sesión | 0 | 3 (parches) |
| Pendientes de autorización | Spring Boot 4.x | React 19, Vite 8, Router 7 |

**Conclusión:** el backend está al día dentro de su línea. En el frontend se corrigieron los parches seguros; lo que queda exige actualizaciones mayores y debe decidirse aparte.

---

## Backend (Maven)

Padre: `spring-boot-starter-parent` **3.5.16** = última versión de la línea 3.5. La siguiente estable es **4.1.2** (salto mayor).

| Dependencia | Actual | Última | Tipo | Acción |
| --- | --- | --- | --- | --- |
| `spring-boot-starter-parent` | 3.5.16 | 4.1.2 | Mayor | **No ahora** (rompe Spring Security 7 y configuración) |
| `spring-boot-starter-web` | 3.5.16 (BOM) | 4.2.0-RC2 | Mayor / RC | No |
| `spring-boot-starter-security` | 3.5.16 (BOM) | 4.2.0-RC2 | Mayor / RC | No |
| `spring-boot-starter-data-jpa` | 3.5.16 (BOM) | 4.2.0-RC2 | Mayor / RC | No |
| `spring-boot-starter-validation` | 3.5.16 (BOM) | 4.2.0-RC2 | Mayor / RC | No |
| `spring-boot-starter-mail` | 3.5.16 (BOM) | 4.2.0-RC2 | Mayor / RC | No |
| `spring-boot-starter-thymeleaf` | 3.5.16 (BOM) | 4.2.0-RC2 | Mayor / RC | No |
| `com.h2database:h2` | 2.3.232 (BOM) | 2.5.252 | Menor | Se actualiza subiendo el BOM |
| `com.mysql:mysql-connector-j` | 9.7.0 (BOM) | 26.10.0 | Mayor | No (cambio de numeración) |
| `org.flywaydb:flyway-core` / `-mysql` | 11.7.2 (BOM) | 13.12.1 | Mayor | No |

**Nota:** casi todas las versiones las administra el `parent`. No se deben fijar versiones a mano: se actualizan subiendo la versión del padre.

**Pendiente:** no se ejecutó un análisis de vulnerabilidades de Maven (OWASP Dependency-Check). Requiere descargar la base NVD (varios minutos).

---

## Frontend (npm)

| Paquete | Actual | Última | Tipo | Acción | Resultado |
| --- | --- | --- | --- | --- | --- |
| `bootstrap` | 5.3.3 | 5.3.8 | Parche | Actualizar | ✅ 5.3.8 |
| `react-router-dom` | 6.30.0 | 6.30.6 | Parche | Actualizar | ✅ 6.30.6 |
| `vite` | 5.4.11 | 5.4.21 | Parche | Actualizar | ✅ 5.4.21 |
| `react` | 18.3.1 | 19.3.1 | **Mayor** | Autorizar | ⏸ Pendiente |
| `react-dom` | 18.3.1 | 19.3.1 | **Mayor** | Autorizar | ⏸ Pendiente |
| `@vitejs/plugin-react` | 4.3.4 | 6.2.1 | **Mayor** | Autorizar | ⏸ Pendiente |
| `react-router-dom` | 6.30.6 | 7.18.5 | **Mayor** | Autorizar | ⏸ Pendiente |
| `vite` | 5.4.21 | 8.3.6 | **Mayor** | Autorizar | ⏸ Pendiente |

(El router y Vite aparecen dos veces: una por el parche aplicado y otra por el salto mayor disponible.)

---

## Actualizadas en esta sesión

| Dependencia | Antes | Después | Verificación |
| --- | --- | --- | --- |
| `bootstrap` | 5.3.3 | 5.3.8 | `npm run build` OK |
| `react-router-dom` | 6.30.0 | 6.30.6 | `npm run build` OK |
| `vite` | 5.4.11 | 5.4.21 | `npm run build` OK |

Build: **OK** — 55 módulos, `dist/` generado en 6,11 s.

---

## Vulnerabilidades restantes

| Paquete | Severidad | Problema | Solución | Riesgo real |
| --- | --- | --- | --- | --- |
| `vite` | **Alta** | Path traversal y `server.fs.deny` en el servidor de desarrollo; `launch-editor` en Windows | `vite@8` (mayor) | Solo afecta `npm run dev`, no al sitio publicado |
| `esbuild` | Moderada | El servidor de desarrollo permite leer respuestas | `vite@8` (mayor) | Solo desarrollo |
| `react-router-dom` | Moderada | Open redirect con `\` y CVE-2025-68470; hidratación SSR | `react-router-dom@7` (mayor) | La app es SPA sin SSR; el redirect requiere un enlace manipulado |

**Lectura práctica:** ninguna de las 4 vulnerabilidades restantes afecta al sitio ya compilado y publicado. Las dos de Vite son del servidor de desarrollo local. La del router es de menor impacto en una SPA, pero conviene planear el salto a la versión 7.

---

## Pendientes de tu decisión (actualizaciones mayores)

| Dependencia | Actual | Propuesta | Por qué esperar |
| --- | --- | --- | --- |
| `react` + `react-dom` | 18.3.1 | 19.3.1 | Cambios en `createRoot`, refs y tipos; requiere revisar todos los componentes |
| `react-router-dom` | 6.30.6 | 7.18.5 | Cambia la API de rutas y `loaders`; hay que migrar `App.jsx` |
| `vite` + `@vitejs/plugin-react` | 5.4.21 / 4.3.4 | 8.3.6 / 6.2.1 | Node 20+, nueva configuración; corregiría las 4 vulnerabilidades |
| `spring-boot-starter-parent` | 3.5.16 | 4.1.2 | Spring Security 7, Jakarta y configuración de sesión; migración grande |

**Recomendación para el lunes:** publicar con lo actual. Las 3 actualizaciones aplicadas son seguras y el build está en verde. Las mayores se planean como tarea aparte, con pruebas.

---

## Cómo repetir la auditoría

```powershell
# Backend
$env:JAVA_HOME="C:\Users\ac765\.jdks\openjdk-25.0.1"
& "C:\Users\ac765\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd" `
  -B -f backend\pom.xml versions:display-dependency-updates

# Frontend
npm --prefix frontend outdated
npm --prefix frontend audit
```

O pide al subagente `auditor-dependencias` que lo haga y escriba el informe.
