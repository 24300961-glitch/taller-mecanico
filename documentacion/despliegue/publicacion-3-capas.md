# Publicación en 3 capas — frontend, API y base de datos

Objetivo: publicar el proyecto en internet con **tres piezas separadas** y la **base de datos privada**.

```
Navegador
    │  HTTPS (un solo dominio público)
    ▼
┌─────────────────────────────┐
│ Entrada pública (proxy)     │  decide a dónde va cada petición
└──────────┬──────────────────┘
           │
     ┌─────┴──────┐
     ▼            ▼
 Frontend      API Spring Boot        Base de datos MySQL
 (estático)    (contenedor)           (red privada)
 Vercel        Render                 Aiven
```

La base de datos **no** se publica: solo la API la alcanza por red privada.

---

## 1. Por qué hace falta un proxy

La aplicación usa **sesión con cookie** (`JSESSIONID`) y **CSRF**. Si el frontend y la API quedan en dominios distintos:

- la cookie se vuelve de terceros y los navegadores la bloquean;
- `fetch(..., { credentials: 'same-origin' })` no envía nada;
- el CSRF falla.

Además, la ruta `/login` se usa de dos formas:

| Petición | Uso |
| --- | --- |
| `GET /login` | Página React (SPA) |
| `POST /login` | Autenticación del backend |

Por eso el proxy debe enviar al backend **solo** `POST /login`, y el resto de `/login` al frontend.

---

## 2. Proveedores recomendados

| Capa | Proveedor | Plan | Por qué |
| --- | --- | --- | --- |
| Frontend | **Vercel** | Hobby (gratis) | Build de Vite, HTTPS y dominio propio |
| API | **Render** (Web Service, Docker) | Free / Starter | Usa el `Dockerfile` tal como está |
| Base de datos | **Aiven for MySQL** | Free | MySQL 8, TLS y red privada |
| Entrada pública | **Cloudflare** (DNS + Worker) | Free | Enruta por ruta y método sin cambiar código |

Alternativas válidas: Railway (API + MySQL), Fly.io (API), Netlify (frontend), Clever Cloud (MySQL). No mezclar proveedores de BD y API en la misma red si no se controla.

### 2.1 Proveedores .NET (MonsterASP, Somee)

| Proveedor | ¿Sirve para el frontend? | ¿Sirve para la API? | ¿Sirve para la BD? |
| --- | --- | --- | --- |
| **Netlify** | ✅ Sí (estático, gratis, uso comercial) | ❌ No corre Java | ❌ No ofrece MySQL |
| **MonsterASP.NET** (free) | ✅ Sí (sitio estático, 5 GB) | ❌ No: sin Java y sin Docker | ✅ Sí: MySQL/MariaDB 10.10, 1 GB, solo UE |
| **Somee** (free) | ⚠️ Sí, pero con anuncios y 150 MB | ❌ No: solo .NET | ❌ Solo MSSQL de 30 MB |

**Regla:** un proveedor de hosting .NET no puede ejecutar Spring Boot. Para la API hace falta un servicio con Java o Docker.

**Si se usa MonsterASP para la BD:** es MariaDB 10.10 (compatible con MySQL 8.0). Hay que activar el acceso remoto, usar TLS y **probar las migraciones Flyway**; no tiene respaldos automáticos en el plan gratis.


---

## 3. Variables de entorno de la API (Render)

| Variable | Ejemplo | Obligatoria |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://HOST:3306/DB?sslMode=REQUIRED&serverTimezone=UTC&characterEncoding=UTF-8` | Sí |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | Sí |
| `DB_USERNAME` | `avnadmin` | Sí |
| `DB_PASSWORD` | `********` | Sí |
| `DB_DIALECT` | `org.hibernate.dialect.MariaDBDialect` (MonsterASP) o `org.hibernate.dialect.MySQLDialect` (MySQL 8) | **Sí** |
| `DB_DDL_AUTO` | `validate` | **Sí** |
| `DB_FLYWAY_ENABLED` | `true` | **Sí** |
| `H2_CONSOLE_ENABLED` | `false` | Sí |
| `APP_BASE_URL` | `https://app.tudominio.com` | Sí |
| `APP_ADMIN_EMAIL` | `admin@taller.local` | Sí |
| `APP_ADMIN_PASSWORD` | mínimo 16 caracteres | Sí (crea el admin) |
| `TALLERES_STORAGE` | `/app/storage/talleres` | Sí (con disco) |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.tudominio.com` / `587` | Para correos |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | `********` | Para correos |
| `JAVA_OPTS` | `-Xms256m -Xmx512m -XX:+UseSerialGC` | Recomendada |

> **Crítico:** sin `DB_DDL_AUTO=validate`, Hibernate usa `create-drop` y puede **borrar las tablas** al apagar. Sin `DB_DIALECT`, usa el dialecto de H2 y falla contra MySQL/MariaDB.
>
> Sin `DB_URL`, la app arranca con H2 en memoria y **pierde todo al reiniciar**.
>
> El administrador solo se crea si `APP_ADMIN_PASSWORD` tiene **16 caracteres o más**.

---

## 4. Paso a paso

### Paso 1 — Base de datos (Aiven)

1. Crear servicio **MySQL 8** en Aiven.
2. Crear la base `taller`.
3. Copiar host, puerto, usuario y contraseña.
4. Descargar el certificado `ca.pem` si Aiven lo exige.
5. **No** abrir el acceso público si el plan lo permite; si no, restringir por IP de Render.

### Paso 2 — API (Render)

1. Conectar el repositorio de GitHub.
2. Crear **Web Service → Docker**.
3. Dockerfile: `./Dockerfile`.
4. Agregar las variables de la tabla anterior.
5. **Disco persistente** montado en `/app/storage` (las fotos de talleres se guardan en disco).
6. Health check: `/` o `/api/documentacion`.
7. Anotar la URL: `https://taller-api.onrender.com`.

### Paso 3 — Frontend (Vercel)

1. Importar el repositorio.
2. **Root Directory:** `frontend`.
3. Framework: **Vite**.
4. Build: `npm run build`. Output: `dist`.
5. Deploy. Anotar la URL: `https://taller.vercel.app`.
6. El archivo `frontend/vercel.json` ya deja las rutas de React funcionando.

### Paso 4 — Entrada única (Cloudflare Worker)

1. Agregar el dominio a Cloudflare.
2. Crear un Worker con el dominio `app.tudominio.com`.
3. Pegar este código (cambiar las dos URLs):

```js
const FRONTEND = 'https://taller.vercel.app';
const BACKEND  = 'https://taller-api.onrender.com';

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const p = url.pathname;
    const vaAlBackend =
      p.startsWith('/api/') ||
      p.startsWith('/storage/') ||
      p === '/logout' ||
      (p === '/login' && request.method === 'POST');

    const destino = vaAlBackend ? BACKEND : FRONTEND;
    const nueva = new URL(url.pathname + url.search, destino);

    const headers = new Headers(request.headers);
    headers.set('X-Forwarded-Host', url.host);

    const res = await fetch(nueva, {
      method: request.method,
      headers,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
      redirect: 'manual',
    });

    // Conserva cookies y redirecciones relativas en el dominio público.
    const salida = new Headers(res.headers);
    const location = salida.get('location');
    if (location && location.startsWith(BACKEND)) {
      salida.set('location', location.replace(BACKEND, url.origin));
    }
    return new Response(res.body, { status: res.status, headers: salida });
  },
};
```

4. Apuntar el dominio `app.tudominio.com` al Worker.
5. Probar en este orden.

### Paso 5 — Comunicación

Con el Worker, el navegador solo habla con `https://app.tudominio.com`:

| Ruta | Destino |
| --- | --- |
| `GET /login`, `GET /dashboard`, `GET /clientes` | Vercel |
| `POST /login`, `POST /logout` | Render |
| `/api/*`, `/storage/*` | Render |
| MySQL | Solo Render, por red privada |

---

## 5. Verificación en vivo (obligatoria)

| # | Prueba | Resultado esperado |
| --- | --- | --- |
| 1 | `GET https://app.tudominio.com/` | Carga el login |
| 2 | Login con el administrador | Entra al dashboard |
| 3 | Recargar la página (F5) | La sesión sigue activa |
| 4 | `GET /api/auth/me` desde el navegador | Devuelve el usuario |
| 5 | Listar clientes | Muestra los datos de MySQL |
| 6 | `POST /logout` | Cierra sesión y redirige |
| 7 | Abrir la URL de Render directamente | La API responde (no la UI) |
| 8 | Intentar conectar a MySQL desde fuera | **Debe fallar** |
| 9 | Reiniciar el servicio de Render | Los datos siguen (MySQL) y las fotos siguen (disco) |
| 10 | `GET /storage/<foto>` | Devuelve la imagen |

---

## 6. Riesgos y pendientes

| Riesgo | Impacto | Solución |
| --- | --- | --- |
| Fotos en disco efímero | Se pierden al redesplegar | Disco persistente en Render o almacenamiento S3/Cloudinary |
| Correo sin SMTP | No llegan recuperaciones de contraseña | Configurar `MAIL_*` |
| Flyway desactivado | Tablas vacías | `DB_FLYWAY_ENABLED=true` |
| Render free se duerme | Primera carga lenta (~50 s) | Plan Starter o ping periódico |
| Datos iniciales (`DataLoader`) | Puede duplicar datos al reiniciar | Verificar que sea idempotente antes de publicar |
| Dominio y certificados | HTTPS inválido | Usar el dominio de Cloudflare/Vercel |

---

## 7. Lo que se necesita para ejecutarlo

Yo no puedo crear cuentas ni pagar servicios. Para dejar la publicación hecha necesito:

1. Cuenta de **Render** (o Railway).
2. Cuenta de **Aiven** (o el proveedor de MySQL elegido).
3. Cuenta de **Vercel** (o Netlify).
4. Dominio (opcional, pero recomendado) y acceso a **Cloudflare**.
5. Los secretos de la base y del correo.

Con eso, el orden de ejecución es: **BD → API → frontend → proxy → verificación**.

---

## 8. Modo de respaldo (sin proxy)

Si no se quiere configurar el Worker:

- Compilar el frontend dentro del backend (el `Dockerfile` ya lo hace).
- Publicar **solo** el backend en Render y la base en Aiven.
- Resultado: un solo origen, sin CORS y sin problemas de cookies.
- Contra: frontend y API quedan en el mismo servicio.
