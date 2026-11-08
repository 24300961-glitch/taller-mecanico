# Estructura del frontend — React + Vite

Mapa real de `frontend/src/`. Si el código cambia, este documento se actualiza.

## Capas

```
main.jsx → App.jsx → pages/* → components/*
                ↓
              api.js  →  backend /api/*
                ↓
             auth.jsx (sesión y rol)
```

## Archivos base

| Archivo | Responsabilidad |
| --- | --- |
| `main.jsx` | Punto de entrada. Monta React. |
| `App.jsx` | Rutas y protección por rol. |
| `api.js` | Única capa de `fetch`. CSRF, credenciales y errores. |
| `auth.jsx` | Contexto de sesión: usuario, rol, login, logout. |
| `styles.css` | Estilos globales del proyecto. |

## Páginas (`src/pages/`)

| Página | Ruta | Uso |
| --- | --- | --- |
| `Login.jsx` | `/login` | Acceso |
| `Registro.jsx` | `/registro` | Alta de cliente |
| `Recuperar.jsx` | `/recuperar` | Solicitar recuperación |
| `ResetPassword.jsx` | `/reset-password` | Cambiar contraseña |
| `Dashboard.jsx` | `/` | Panel según rol |
| `Clientes.jsx` | `/clientes` | Listado y alta de clientes |
| `Vehiculos.jsx` | `/vehiculos` | Listado y alta de vehículos |
| `Talleres.jsx` | `/talleres` | Talleres |
| `Usuarios.jsx` | `/usuarios` | Administración de usuarios |
| `Roles.jsx` | `/roles` | Roles y permisos |
| `Historial.jsx` | `/historial` | Historial de vehículo |
| `Documentacion.jsx` | `/documentacion` | Documentación de la API |

## Componentes (`src/components/`)

| Componente | Responsabilidad |
| --- | --- |
| `Layout.jsx` | Marco de la app: navegación y contenido. |
| `BrandPanel.jsx` | Panel de marca en pantallas de acceso. |
| `ComboTaller.jsx` | Selector de taller. |
| `ui.jsx` | Componentes de UI compartidos (botones, campos, avisos). |

## Contratos de API usados

Todas las llamadas son relativas y con credenciales de sesión:

```
/api/auth/*        sesión y contraseña
/api/clientes*     clientes
/api/vehiculos*    vehículos
/api/talleres*     talleres
/api/admin/*       usuarios y roles
/api/sepomex/*     estados y códigos postales
/api/documentacion
```

## Reglas de estructura

1. Si un componente llama a `fetch`, está mal: debe usar `api.js`.
2. Una página no contiene componentes reutilizables: esos van en `components/`.
3. La protección por rol se declara en `App.jsx`; el backend vuelve a validar.
4. No hay carpetas vacías.

## Build

- `npm run dev` → desarrollo.
- `npm run build` → `frontend/dist/`.
- Para publicar en un solo origen, el `dist/` se copia a `backend/src/main/resources/static/`.
- Para publicar separado, ver `documentacion/despliegue/publicacion-3-capas.md`.
