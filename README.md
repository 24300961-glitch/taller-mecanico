# Portal del Taller Norte

Aplicación base para un taller mecánico: interfaz web en español, registro e inicio de sesión, roles, restablecimiento de contraseña por correo, MySQL y registro de errores.

## Tecnologías

- Java 25 y Spring Boot 3.5
- Spring Security con sesiones y contraseñas BCrypt
- Spring Data JPA y MySQL 8.4
- Flyway para migraciones MySQL
- HTML, CSS y JavaScript servidos desde Spring Boot
- Bootstrap 5.3.8 por CDN oficial, más estilos propios del portal
- Docker Compose para levantar MySQL y la aplicación

## Requisitos

- Docker Desktop con Compose
- Para desarrollo desde IntelliJ IDEA: JDK 25 y Maven 3.6.3 o posterior
- Una cuenta SMTP para habilitar correos de recuperación

## Arranque con Docker

1. Copia `.env.example` a `.env` y cambia las contraseñas de MySQL y la contraseña inicial del administrador. Usa secretos únicos de al menos 16 caracteres para el administrador. No subas `.env` al control de versiones.
2. Completa las variables SMTP en `.env` para que funcione la recuperación de contraseñas. Usa credenciales de aplicación del proveedor, no la contraseña personal del correo.
3. Desde la raíz del proyecto ejecuta:

   ```powershell
   docker compose up --build
   ```

4. Abre [http://localhost:8080](http://localhost:8080). Para detener los servicios usa `Ctrl+C` y `docker compose down`. La base de datos se conserva en el volumen `mysql_data`.

La SPA se desarrolla en `frontend/src` y Vite genera `frontend/dist`. En la imagen Docker, el `Dockerfile` copia esa salida a `backend/src/main/resources/static/`; Spring Boot sirve `index.html` y sus bundles bajo `/assets/` en el mismo origen. El `index.html` y los bundles que hoy aparecen en `static/` son salida generada y no deben editarse como fuente de la interfaz.

Sin SMTP configurado, el inicio de sesión, el registro y los roles funcionan, pero no se pueden enviar enlaces de recuperación. La aplicación no imprime enlaces ni contraseñas en los logs.

## Arranque desde IntelliJ IDEA

1. Configura JDK 25 en Project Structure y abre este proyecto como Maven.
2. Levanta únicamente MySQL: `docker compose up -d mysql`.
3. Define las variables `DB_URL=jdbc:mysql://localhost:3307/taller_mecanico?serverTimezone=UTC`, `DB_DRIVER=com.mysql.cj.jdbc.Driver`, `DB_DIALECT=org.hibernate.dialect.MySQLDialect`, `DB_DDL_AUTO=validate`, `DB_FLYWAY_ENABLED=true`, `DB_USERNAME=taller`, `DB_PASSWORD` (el mismo valor de `.env`), `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD`, `APP_BASE_URL` y las variables `MAIL_*` en la configuración de ejecución.
4. Ejecuta `mx.tallermecanico.app.TallerMecanicoApplication`.

La cuenta inicial de administrador se crea solo si `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` están configurados y el correo aún no existe. Las cuentas creadas desde la página pública siempre son clientes. El administrador puede asignar los roles desde el panel o mediante `PATCH /api/admin/users/{id}/role`.

## Funcionalidad incluida

- Alta pública de clientes; correo único y validación de contraseña de 12 a 72 caracteres.
- Inicio/cierre de sesión con cookie de sesión de Spring Security.
- Roles `ADMINISTRADOR`, `RECEPCIONISTA`, `CLIENTE` y `MAESTRO_MECANICO`.
- Panel inicial de administración para consultar usuarios y cambiar roles.
- Recuperación mediante enlace enviado al correo, token aleatorio de un uso con vencimiento de 15 minutos. La base almacena el hash SHA-256 del token, nunca el token original.
- Respuestas genéricas en la solicitud de recuperación para no revelar si un correo está registrado.
- Errores controlados con respuesta segura e identificador de incidente; detalles técnicos solo en `logs/taller-mecanico.log`, con rotación diaria/tamaño e historial limitado.
- Gestión de talleres por Administrador, clientes asociados a uno o más talleres y vehículos con un taller asignado propio.
- Catálogo SEPOMEX local opcional, importado desde archivo TXT delimitado por `|` o CSV.

## Seguridad de contraseñas

Las contraseñas no se cifran con AES ni se guardan en texto plano: se almacenan como hashes BCrypt con costo 12 y sal generada por la biblioteca. SHA-512 simple tampoco es apropiado para almacenar contraseñas. BCrypt es una función adaptativa de una sola vía compatible con Spring Security. El token de recuperación usa SHA-256 únicamente porque es un valor aleatorio de alta entropía y su original solo se envía por correo.

## Endpoints principales

| Método | Ruta | Acceso | Uso |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Crear cuenta de cliente |
| `POST` | `/api/auth/login` | Público | Iniciar sesión (`email`, `password` como formulario URL encoded) |
| `POST` | `/api/auth/logout` | Sesión | Cerrar sesión |
| `GET` | `/api/auth/me` | Sesión | Consultar perfil actual |
| `POST` | `/api/auth/forgot-password` | Público | Solicitar enlace (`email`) |
| `POST` | `/api/auth/reset-password` | Público | Cambiar clave (`token`, `newPassword`) |
| `GET` | `/api/admin/users` | Administrador | Listar cuentas |
| `PATCH` | `/api/admin/users/{id}/role` | Administrador | Asignar rol (`role`) |
| `GET` | `/api/talleres` | Administrador | Consultar talleres |
| `POST` | `/api/talleres` | Administrador | Registrar taller multipart con foto opcional |
| `PATCH` | `/api/talleres/{id}/desactivar` | Administrador | Desactivar taller sin borrar relaciones |
| `PATCH` | `/api/talleres/{id}/reactivar` | Administrador | Reactivar taller |
| `DELETE` | `/api/talleres/{id}` | Administrador | Borrado bloqueado; sugiere desactivar |
| `GET` | `/api/codigos-postales/{cp}` | Administrador | Consultar catálogo local SEPOMEX |
| `POST` | `/api/clientes` | Administrador/Recepcionista | Crear cliente nuevo; email existente responde 409 |
| `POST` | `/api/vehiculos` | Administrador/Recepcionista | Registrar vehículo con su taller específico |
| `GET` | `/api/vehiculos/mis` | Cliente | Consultar sus vehículos y taller asignado |

## Estructura actual

```text
AGENTS.md                        guía común para todas las herramientas de IA
CLAUDE.md, GEMINI.md             puentes para Claude Code y Gemini CLI
CONVENTIONS.md                   puente para Aider
.github/copilot-instructions.md  puente para GitHub Copilot
.cursor/, .windsurf/, .clinerules, .roo/   puentes para esas herramientas
.opencode/agents/                5 agentes para OpenCode
.claude/agents/                  los mismos 5 agentes para Claude Code
backend/                         API Spring Boot (Java 25, puerto 8080)
  src/main/java/mx/tallermecanico/app/
    audit/ auth/ clientes/ common/ config/ dashboard/ error/
    seguridad/ sepomex/ taller/ usuarios/ vehiculos/ web/
  src/main/resources/
    db/migration/                 migraciones Flyway V1–V6
    static/                       index.html, css/, js/, assets/, diagramas/
    templates/error.html          página de error
  pom.xml                         proyecto Maven
frontend/                        SPA React + Vite (puerto 5173 en desarrollo)
  src/pages/                      páginas de la aplicación
  src/components/                 componentes compartidos
  src/                            App.jsx, api.js, auth.jsx, estilos
  dist/                           salida compilada (generada; ignorada por Git)
Spec/
  arquitectura/orquestador.md     reglas de flujo y alcance para tareas
  SKILL/Backend/skills.md         guía técnica de Spring Boot y REST
  SKILL/Frontend/frontend.md      guía frontend
  Agentes/                        instrucciones QA y agentes
documentacion/
  backend/                        documentador, mejores prácticas y estructura backend
  frontend/                       documentador, mejores prácticas y estructura frontend
  despliegue/                     publicación en 3 capas
  auditoria/                      informes de dependencias
diagramas/                        documentación de arquitectura y flujos
tablas/                           documentación de tablas y modelo de datos
database/migrations/              carpeta auxiliar; actualmente vacía
docker/mysql-init/                 inicialización de MySQL para Docker
data/sepomex/                     entrada local opcional del catálogo postal
storage/talleres/                 fotos persistidas localmente
logs/, logs-boot/                 registros locales de ejecución
compose.yaml, Dockerfile           configuración de contenedores
```

Las migraciones activas están únicamente en `backend/src/main/resources/db/migration`; `database/migrations/` está vacía y no es la ubicación configurada para Flyway. `frontend/dist/`, `frontend/node_modules/`, `backend/target/`, `.build/` y `target/` son artefactos o dependencias generados, no módulos fuente.

Para seguir las reglas de trabajo, la entrada común es `AGENTS.md` (raíz). Después, el contrato completo está en `Spec/arquitectura/orquestador.md`. En tareas de Spring Boot carga también `Spec/SKILL/Backend/skills.md`; para cambios de interfaz, consulta `Spec/SKILL/Frontend/frontend.md`.

## Compatibilidad con herramientas de IA

No existe un formato único de agentes entre programas. La solución es una guía común (`AGENTS.md`) y un archivo puente por herramienta:

| Herramienta | Archivo que lee | Agentes |
| --- | --- | --- |
| OpenCode | `AGENTS.md` | `.opencode/agents/*.md` |
| Claude Code | `CLAUDE.md` → `@AGENTS.md` | `.claude/agents/*.md` |
| Codex | `AGENTS.md` | — (lee el catálogo) |
| Cursor | `.cursor/rules/proyecto.mdc` | — |
| GitHub Copilot | `.github/copilot-instructions.md` | — |
| Gemini CLI | `GEMINI.md` | — |
| Windsurf | `.windsurf/rules/proyecto.md` | — |
| Cline | `.clinerules` | — |
| Roo Code | `.roo/rules/00-proyecto.md` | — |
| Aider | `CONVENTIONS.md` | — |

Regla: **el contenido vive una sola vez**. Los archivos puente solo apuntan a `AGENTS.md` y a los contratos de `Spec/` y `documentacion/`. Si cambias una regla, se cambia en `AGENTS.md` o en el contrato, no en cada puente.

## Talleres, clientes y vehículos

- Las migraciones están en `backend/src/main/resources/db/migration`; Compose las aplica con Flyway y luego Hibernate valida el esquema.
- La app ya no crea talleres al arrancar. El Administrador registra el primero en `/talleres`.
- Para poblar CP, descarga el catálogo de [Correos de México](https://www.correosdemexico.gob.mx/sslservicios/consultacp/CodigoPostal_Exportar.aspx), monta el archivo bajo `data/sepomex/`, define `SEPOMEX_FILE` con su ruta real dentro del contenedor y habilita `SEPOMEX_IMPORTAR=true` una sola vez. Detalles y formato se describen en `casos-de-uso.md`.
- Las fotos se guardan en el volumen local `storage/talleres`; MySQL guarda la ruta.
- Los registros anteriores se conservan. Las asociaciones nuevas requieren un taller activo y completo; las relaciones de vehículos antiguos pueden asignarse desde su edición.

## Strategy por área

- `auth/strategy`: `StrategyAuthenticationProvider` selecciona la estrategia con base en la autoridad `ROLE_*`; cada rol configurado recibe una estrategia BCrypt.
- `usuarios/strategy`: `UserCreationService` delega la construcción en una estrategia que corresponde al rol solicitado. El registro público solo consulta el rol configurado para clientes; el alta administrativa requiere `ROLES_ASIGNAR`.
- `auth/strategy`: recuperación selecciona una estrategia por nombre de canal. EMAIL requiere SMTP. SMS requiere una implementación Spring de `SmsGateway` y un teléfono previamente verificado; hasta entonces el canal falla cerrado. No se imprime el token en logs.
- `dashboard/strategy`: se conservan las implementaciones existentes; el panel React actual muestra únicamente el saludo y la insignia del rol. `GET /api/dashboard` entrega solo la identidad, sin tarjetas ni accesos rápidos.

Todos los métodos nuevos de estrategia llevan Javadoc con propósito, parámetros, retorno y excepciones pertinentes; las decisiones de seguridad están comentadas inline.

## Añadir una estrategia

- **Nuevo canal de recuperación:** agrega una clase `@Component` que implemente `RecoveryDeliveryStrategy`, cuyo método `supports(String channel)` reconozca su clave y cuyo `deliver(...)` utilice un gateway seguro. No cambies el coordinador: Spring inyecta la colección de estrategias.
- **Nueva vista de rol:** el panel actual comparte la vista compacta para todos los roles; cualquier cambio de diseño se documenta antes de añadir datos o accesos al dashboard.
- **Nuevo rol de usuario:** añade su definición a `app.seguridad.roles-iniciales`. Las configuraciones crean estrategias de autenticación y alta para cada rol definido; la vista requiere su estrategia y template propio.
- **Nueva modalidad de login:** agrega un bean `@Component` que implemente `AutenticacionStrategy` y restringe el matching a la autoridad o credencial correcta. Las estrategias extendidas tienen prioridad sobre las estrategias BCrypt base.
- **Reglas de creación especiales:** agrega un bean `@Component` que implemente `CreacionUsuarioStrategy`; el selector consulta las estrategias extendidas antes de la estrategia configurada genérica.

Para habilitar SMS en producción, proporciona un bean `SmsGateway` respaldado por un proveedor real y un proceso OTP separado para verificar el número del usuario. La cuenta guarda el indicador de verificación como `false` por defecto y la Strategy se niega a mandar enlaces a teléfonos sin verificar.
