# Análisis de vulnerabilidades — 2026-11-10

## Resumen

| Herramienta | Alcance | Resultado |
| --- | --- | --- |
| `npm audit` (npm 11.19.0) | Dependencias del frontend | **4** avisos: 1 alta, 3 moderadas |
| Trivy 0.76.2 | `backend/pom.xml`, `frontend/package-lock.json` y secretos | **13** HIGH/CRITICAL en Maven (7 críticas, 6 altas); **0** en npm; **0** secretos |
| Gitleaks 8.30.1 | Historial Git (15 commits) | **1** hallazgo: falso positivo en `.env.example` |
| OSS Index | Dependencias del backend | **No ejecutado**: el servicio respondió `401 Unauthorized` sin cuenta |

**Conclusión:** el frontend solo tiene un aviso alto que afecta al **servidor de desarrollo** (Vite), no al sitio publicado. El riesgo real está en el **backend**: 13 vulnerabilidades altas o críticas en Tomcat, Jackson y Spring WebMVC. Once de las trece se corrigen fijando dos versiones de parche; las dos restantes exigen Spring Boot 4.x, un salto mayor que debe planearse aparte.

---

## 1. Backend (Maven) — Trivy

Versiones resueltas confirmadas con `mvn dependency:list`:

| Artefacto | Versión instalada |
| --- | --- |
| `org.apache.tomcat.embed:tomcat-embed-core` | 10.1.55 |
| `org.springframework:spring-webmvc` (y `spring-core`) | 6.2.19 |
| `com.fasterxml.jackson.core:jackson-core` | 2.21.4 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.4 |
| Padre `spring-boot-starter-parent` | 3.5.16 (última de la línea 3.5) |

### Tomcat — 6 hallazgos (5 críticos, 1 alto)

| CVE | Severidad | Título | Versión corregida |
| --- | --- | --- | --- |
| CVE-2026-55276 | Crítica | Registros de seguridad engañosos por flujo de control incorrecto | 10.1.56 |
| CVE-2026-65182 | Crítica | Elusión de restricciones de seguridad por control de acceso incorrecto | 10.1.58 |
| CVE-2026-65637 | Crítica | Elusión de validación estricta en HTTP/2 sin autoridad | 10.1.59 |
| CVE-2026-65905 | Crítica | Elusión de autenticación por ataque de repetición en DIGEST | 10.1.58 |
| CVE-2026-68525 | Crítica | Acceso no autorizado por elusión de autenticación FORM | 10.1.58 |
| CVE-2026-53404 | Alta | Flujo de control incorrecto en la válvula de reescritura | 10.1.56 |

**Impacto en este proyecto:** la aplicación usa Spring Security con sesión y CSRF, **no** la autenticación FORM/DIGEST del contenedor. Aun así, conviene actualizar: Tomcat es el servidor que atiende todas las peticiones.

### Jackson — 5 hallazgos (todos altos)

| CVE | Paquete | Título | Versión corregida |
| --- | --- | --- | --- |
| CVE-2026-89407 | `jackson-core` | Denegación de servicio por backtracking de expresiones regulares | 2.21.7 |
| CVE-2026-89425 | `jackson-core` | Denegación de servicio por crecimiento ilimitado de `StringBuilder` | 2.21.7 |
| CVE-2026-68497 | `jackson-databind` | Denegación de servicio por análisis numérico sin límite | 2.21.6 |
| CVE-2026-91776 | `jackson-databind` | Denegación de servicio por crecimiento ilimitado de caché | 2.21.7 |
| CVE-2026-91777 | `jackson-databind` | Denegación de servicio por referencias adelantadas cuadráticas | 2.21.7 |

**Impacto:** la API recibe y procesa JSON en todos los endpoints, así que un atacante podría intentar agotar CPU o memoria. Es la familia más fácil de corregir.

### Spring WebMVC — 2 hallazgos (críticos)

| CVE | Título | Versión corregida |
| --- | --- | --- |
| CVE-2026-47884 | Ejecución remota de código por limitación incorrecta de rutas en `XsltView` | 7.0.9 |
| CVE-2026-47890 | Corrupción de flujo en Server-Sent Events (SSE) con vistas | 7.0.9 |

**Impacto:** el proyecto usa **Thymeleaf**, no `XsltView`, y **no** usa SSE. La exposición práctica es baja. La corrección real implica **Spring Framework 7**, es decir, **Spring Boot 4.x**, un salto mayor.

---

## 2. Frontend (npm) — `npm audit`

| Paquete | Severidad | Aviso | ¿Afecta al sitio publicado? |
| --- | --- | --- | --- |
| `vite` | **Alta** | Varios: recorrido de rutas en `.map` de dependencias optimizadas, divulgación de hash NTLMv2 en `launch-editor`, ejecución de scripts entre orígenes y elusión de `server.fs.allow` / `server.fs.deny` | **No**: son fallos del servidor de desarrollo (`npm run dev`). Netlify sirve archivos estáticos ya compilados. |
| `esbuild` | Moderada | Cualquier web puede enviar peticiones al servidor de desarrollo y leer la respuesta (GHSA-67mh-4wv8-2f99) | **No**: misma razón. |
| `react-router` | Moderada | Redirección abierta con barra invertida en `<Link>` / `useNavigate` (GHSA-wrjc-x8rr-h8h6) | **Sí, parcialmente**: la app no construye enlaces con datos del usuario, pero conviene corregir. |
| `react-router` | Moderada | Inyección de constructores en `deserializeErrors()` durante la hidratación SSR (GHSA-337j-9hxr-rhxg) | **No**: la app no usa SSR. |

**No ejecutar `npm audit fix --force`:** subiría `vite` 5 → 8 y `react-router` 6 → 7, ambos cambios mayores que requieren probar la aplicación.

> Trivy reportó **0 vulnerabilidades** en `frontend/package-lock.json`. La diferencia se debe a que Trivy y npm usan fuentes de avisos distintas; para npm, `npm audit` es la referencia.

---

## 3. Secretos — Gitleaks

| Archivo | Regla | Resultado |
| --- | --- | --- |
| `.env.example` línea 9 | `generic-api-key` | **Falso positivo**: `APP_ADMIN_PASSWORD` contiene un texto de ayuda, no una credencial real |

Se añadió `.gitleaksignore` con la huella del hallazgo y una nota explicando que es un marcador de ejemplo. El resto del historial (15 commits) no contiene secretos detectables.

> La contraseña de la base de datos compartida en el chat **no aparece** en el repositorio según Gitleaks. Aun así, debe rotarse por precaución.

---

## 4. Acciones aplicadas en esta sesión

| Acción | Archivo |
| --- | --- |
| Actualizaciones automáticas de dependencias (Maven, npm, Docker y GitHub Actions), solo parches y menores | `.github/dependabot.yml` |
| Análisis estático de Java y JavaScript en cada push, PR y semanalmente | `.github/workflows/codeql.yml` |
| Registro del falso positivo de Gitleaks | `.gitleaksignore` |

### Qué queda pendiente en GitHub (requiere permisos de administrador)

1. **Settings → Code security**: activar *Dependabot alerts* y *Dependabot security updates*.
2. **Settings → Code security**: verificar que *CodeQL* esté habilitado (el flujo de trabajo ya lo ejecuta).
3. **Settings → Code security**: activar *Secret scanning* y *Push protection*.

---

## 5. Recomendaciones

### Prioridad alta — corregir 11 de 13 en el backend

Añadir al `backend/pom.xml`, dentro de `<properties>`:

```xml
<tomcat.version>10.1.59</tomcat.version>
<jackson-bom.version>2.21.7</jackson-bom.version>
```

Luego verificar con:

```powershell
$env:JAVA_HOME="C:\Users\ac765\.jdks\openjdk-25.0.1"
cd backend
.\mvnw.cmd -DskipTests compile
```

Esto corrige las 6 de Tomcat y las 5 de Jackson sin cambiar de versión mayor. Conviene probar login, CRUD de vehículos y subida de fotos antes de desplegar.

### Prioridad media — Spring Boot 4.x

Las 2 vulnerabilidades de `spring-webmvc` solo se corrigen con Spring Framework 7. Es un salto mayor que afecta a Spring Security y a la configuración. Debe hacerse en una rama aparte, con pruebas, no como parche urgente.

### Prioridad media — frontend

Planear en una rama la migración a **React 19 + React Router 7 + Vite 8**, ejecutando `npm run build` y una revisión visual completa antes de publicar.

### Prioridad baja — repetir el análisis

- `npm audit` en cada cambio de dependencias.
- CodeQL en cada push (automático con el flujo añadido).
- Trivy y Gitleaks antes de cada publicación.

---

## 6. Comandos utilizados

```powershell
# Frontend
npm --prefix frontend audit

# Backend: versiones resueltas
mvn -f backend/pom.xml dependency:list -DincludeArtifactIds=tomcat-embed-core,spring-webmvc,jackson-core,jackson-databind

# Backend y frontend: vulnerabilidades y secretos
trivy fs --scanners vuln,secret --severity HIGH,CRITICAL --skip-dirs frontend/node_modules --skip-dirs backend/target --skip-dirs .git .

# Historial Git: secretos
gitleaks detect --source . --redact
```

**Versiones:** Java 25.0.1, Node 24.21.0, npm 11.19.0, Trivy 0.76.2, Gitleaks 8.30.1.
