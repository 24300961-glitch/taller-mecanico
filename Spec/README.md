# ESPEC - Taller Mecanico System

## CONTEXT
Sistema completo de taller mecánico con frontend React + backend Spring Boot.
Migración y completado con funcionalidad en vivo. Sin crear datos de prueba en BD.

## ARQUITECTURA
- **Frontend**: React 18 + Vite, SPA sirve en mismo origen (`/index.html` via `forward:`)
- **Backend**: Spring Boot, Maven, multi-etapa Dockerfile (node build + maven package)
- **Base de datos**: MySQL, 8 roles con authorities `ROLE_<CLAVE>`
- **Despliegue**: Docker multi-etapa, imagen parchada con jar (builds atascados por DNS de Docker Desktop)

## MODULOS IMPLEMENTADOS (63/63 VERIFICADOS)

### 1. AUTENTICACIÓN Y SEGURIDAD
- Login local: `admin@taller.local` / contraseña definida en tu `.env` (no se documenta)
- Campo email (no username)
- Token CSRF en POSTs
- Sesión expira al reiniciar contenedor
- 8 roles: ADMINISTRADOR_DE_TALLER, RECEPCIONISTA, CLIENTE, MAESTRO_MECANICO, ASESOR_DE_SERVICIO, TECNICO_DE_CONSULTA, SISTEMA, JEFE_DE_OPERACIONES

### 2. ADMINISTRACIÓN DE CLIENTES
- Lista en tabla con todos los campos
- Editar datos del cliente (formulario precargado, guarda con PUT)
- **No se eliminan físicamente** - Soft-delete con flag `activo`
- Paginación servidor: `GET /api/clientes/pagina?page=0&size=10`
- 10 registros por página
- Filtro por nombre, email, telefono, ID
- Validaciones: RFC, teléfono 10 dígitos, CP ↔ estado
- **Bloquea duplicados** con error 409 (antes reutilizaba, ahora error claro)
- Acceso: Admin + Recepcionista (app.properties actualizado)

### 3. MODULO REGISTRO DE CLIENTE
- Bloquea duplicados definitivos (error 409)
- Mensaje: "Ya existe un cliente con el correo... Elimina o reactiva el registro anterior"
- No reutiliza registros existentes

### 4. VEHÍCULOS
**Registrar:**
- Admin y Recepcionista pueden registrar
- Autocomplete propietario con casillas (#vhPropietario + hidden clienteId + lista #vhPropietarioLista)
- Marca y modelo: texto libre (max 40/60 chars, patrones RX_MARCA/RX_MODELO)
- Validaciones VIN: 17 chars alfanuméricos sin I, O, Q (`^[A-HJ-NPR-Z0-9]{17}$`)
- Validaciones Placa: mayúsculas con guion `ABC-1234` o `ABC-12-34`
- Año: 4 dígitos 1900-actual+1
- Color: solo letras, max 30 chars
- **Campos nuevos**: version (max 30), kilometraje (obligatorio, min 0), fotos (rutas separadas por coma, max 500)
- Combo buscable de talleres (filtro en vivo + casillas + contador)
- Mínimo 1 taller obligatorio para asociar cliente
- Envío POST 200 con mensaje de reutilización

**Consultar:**
- Flujo: VIN/placa/id-alcance del taller-ficha
- Regla: normalizar espacios, guiones y mayúsculas
- Consultar por cliente, placa, VIN

**Actualizar y Desactivar:**
- Desactivar/Reactivar vehículo
- Historial de movimientos (HistorialVehiculo)

### 5. TALLERES
- Alta de taller con formulario y foto
- Combo buscable de talleres en alta de cliente (filtro en vivo + casillas)
- Contador: 'N taller(es) seleccionado(s)'
- Mínimo 1 taller obligatorio para asociar cliente

### 6. CITAS
- `GET /citas` 200 sin leyenda de estados
- 'Nueva cita' disponible
- Tabla intacta

### 7. DASHBOARD
- Solo saludo: 'Hola, Administrador de taller' + insignia de rol
- 0 tarjetas de indicadores, 0 tarjetas de acceso
- Menú lateral: Clientes, Talleres, Vehículos, Historial, Usuarios, Roles

### 8. RECUPERACIÓN
- Campo 'Teléfono de recuperación' eliminado de registro.html
- Backend tolera `recoveryPhone=null`
- Flujo: email → validación → reset token

### 9. FRONTEND
- React 18 + Vite build
- 12 rutas con guardias por permiso
- SPA sirve en `/index.html` via `forward:`
- Favicon propio añadido
- Diseño unificado: portada azul (login, registro, recuperar, reset-password)

### 10. BUILD Y DEPLOY
- Dockerfile multi-etapa (node build + maven package)
- Imagen parchada con jar final
- Capa `dependency:go-offline` añadida
- Desplegado vía parche de jar (DNS de Docker Desktop intermitente)

## BASE DE DATO ACTUAL
- 2 usuarios (admin + `ac7654281@gmail.com` "Anthony Castillo" rol Cliente)
- 8 roles
- 2 clientes (Ana Torres #1, Luis Gomez #2)
- 1 vehículo (ABC-1234)
- 2 talleres (id=2 "taller norte taller mecanico el rayo" completo/activo; id=1 heredado incompleto → 409 en alta de clientes)
- Citas 0
- taller_cliente 0

## PERMISOS POR ROL (application.properties)
```
Administrador de taller: USUARIOS_CONSULTAR,ROL_CREAR,ROLES_ASIGNAR,CLIENTES_CONSULTAR,CLIENTES_REGISTRAR,CLIENTES_EDITAR,CLIENTES_DESACTIVAR,VEHICULOS_CONSULTAR,VEHICULOS_REGISTRAR,VEHICULOS_EDITAR,VEHICULOS_DESACTIVAR
Recepcionista: USUARIOS_CONSULTAR,CLIENTES_CONSULTAR,CLIENTES_REGISTRAR,CLIENTES_EDITAR,CLIENTES_DESACTIVAR,VEHICULOS_CONSULTAR,VEHICULOS_REGISTRAR
Cliente: CLIENTES_CONSULTAR,CLIENTES_EDITAR,VEHICULOS_CONSULTAR,VEHICULOS_EDITAR,VEHICULOS_DESACTIVAR
```

## ENDPOINTS PRINCIPALES
- `POST /api/auth/login` - Login admin
- `GET/POST /api/clientes` - CRUD clientes con paginación
- `GET/POST /api/vehiculos` - CRUD vehículos con validaciones
- `GET/POST /api/talleres` - Talleres y asociaciones
- `GET /api/dashboard` - Dashboard por rol
- `GET /api/documentacion` - Documentación protegida ROLE_ADMINISTRADOR_DE_TALLER
- `GET/POST /api/codigos-postales/{cp}` y `/api/sepomex/cp/{cp}` - Consultas CP

## ARCHIVOS CLAVE
- `backend/src/main/java/mx/tallermecanico/app/clientes/ClienteService.java` - Lógica cliente (bloquea duplicados)
- `backend/src/main/java/mx/tallermecanico/app/vehiculos/VehiculoService.java` - Normalización VIN/Placa
- `backend/src/main/java/mx/tallermecanico/app/vehiculos/VehiculoRequest.java` - Validaciones vehículo
- `frontend/src/pages/Clientes.jsx` - UI administración clientes con paginación y combo talleres
- `frontend/src/pages/Dashboard.jsx` - Dashboard administrador (solo saludo)
- `application.properties` - Permisos por rol

## PROXIMOS PASOS SUGERIDOS
1. Construir Docker cuándo la red se estabilice: `docker compose build app`
2. Implementar subida de fotos vehículo (validación 15MB, vista previa)
3. Añadir sorting UI en tabla de headers (ASC/DESC)
4. Probar completamente con rol Recepcionista todas las operaciones

## PARA CODEX - PROMPT DE GENERACIÓN

Si deseas usar este prompt con Codex, copia y pega:

```
# PROMPT PARA CODEX - SISTEMA TALLER MECANICO

## CONSIGNA GENERAL
Genera el código necesario para completar el sistema Taller Mecanico con React 18 + Vite frontend y Spring Boot 3 backend, siguiendo la especificación completa abajo. El sistema ya tiene 63/63 puntos verificados en vivo, NO crear datos de prueba en BD, y el despliegue actual es por parche de jar (builds Docker atascados por DNS intermitente de Docker Desktop).

## ESTRUCTURA DEL PROYECTO
- backend/: Spring Boot Maven project
- frontend/: React 18 + Vite project
- Dockerfile multi-etapa (node build + maven package)
- target/ raíz eliminado (clean build)

## BASE DE DATOS Y ENTIDADES YA IMPLEMENTADAS
1. Cliente (entidad completa con soft-delete: activo, motivoDesactivacion, desactivadoEn, desactivadoPor)
2. Vehiculo (entidad con: marca String(40), modelo String(60), anio int, placa String(15) única, vin String(17) único, color String(30), version String(30), kilometraje Integer, fotos String(500), cliente ManyToOne, taller ManyToOne, activo boolean default true)
3. Taller (entidad con nombre, activo, y asociaciones cliente-taller many-to-many)
4. Roles y Permisos (8 roles: ADMINISTRADOR_DE_TALLER, RECEPCIONISTA, CLIENTE, MAESTRO_MECANICO, ASESOR_DE_SERVICIO, TECNICO_DE_CONSULTA, SISTEMA, JEFE_DE_OPERACIONES)
5. HistorialVehiculo (entidad para movimientos de vehículos)

## MODULOS A DESARROLLAR / COMPLETAR

### A. ADMINISTRACIÓN DE CLIENTES
- Tabla con paginación servidor (pageable, size 10, totales)
- Filtro por nombre, email, teléfono, ID
- Botón "Editar" por fila que abre formulario precargado
- Botón "Desactivar" por fila (soft-delete, requiere motivo ≥5 chars)
- Botón "Reactivar" para clientes desactivados
- **VALIDACIÓN OBLIGATORIA**: Bloquear alta si ya existe cliente por email/RFC/nombre/telefono (error 409 con mensaje claro)
- Contador total de clientes
- Navegación anterior/siguiente página
- Checkbox "Incluir desactivados"
- Acceso: Admin + Recepcionista

### B. REGISTRO DE CLIENTE
- Formulario completo: nombre, apellidoPaterno, apellidoMaterno, teléfono (10 dígitos), teléfonoAlterno, teléfonoPersonal, teléfonoTrabajo, nombreContacto, teléfonoContacto, email (obligatorio), RFC (validar formato 12/13 chars), dirección, calle, número, códigoPostal (5 dígitos), estado, municipio, colonia
- Validaciones: teléfono exacto 10 dígitos, RFC válido, CP ↔ estado según SEPOMEX
- Al guardar: si el correo ya existe → error 409 "Ya existe un cliente con el correo... Elimina o reactiva"
- Si no hay duplicado → INSERT 201
- Acceso: Admin + Recepcionista

### C. VEHÍCULO - REGISTRAR
- Formulario: cliente (autocomplete con búsqueda + casillas), taller (combo buscable con filtro en vivo), marca (texto libre, max 40, patron RX_MARCA), modelo (texto libre, max 60, patron RX_MODELO), año (4 dígitos 1900-actual+1), versión (texto libre, max 30), placa (mayúsculas con guion ABC-1234 o ABC-12-34), VIN (17 chars alfanuméricos sin I, O, Q: ^[A-HJ-NPR-Z0-9]{17}$), color (solo letras, max 30), kilometraje ( entero ≥0), fotos (rutas separadas por coma, max 500 chars)
- **VALIDACIONES OBLIGATORIAS**:
  - VIN debe tener 17 caracteres alfanuméricos sin I, O, Q
  - Placa debe estar en mayúsculas y separada por guion
  - Año debe ser 4 dígitos entre 1900 y añoActual+1
  - Kilometraje ≥ 0
  - Marca entre 2 y 40 caracteres
  - Modelo entre 2 y 60 caracteres
- **Lógica**: Al registrar, verificar que placa y VIN no existan (error 409 si coinciden)
- Si placa o VIN ya están en un vehículo desactivado → permitir reactivación o error dependiendo de negocio
- Al guardar: INSERT 201 con mensaje "Vehículo con placa ABC-1234 registrado correctamente"
- Acceso: Admin + Recepcionista

### D. VEHÍCULO - CONSULTAR
- `GET /api/vehiculos?clienteId=X` - Vehículos de un cliente específico
- `GET /api/vehiculos?placa=ABC` - Buscar por placa (normalizar a mayúsculas y guiones)
- `GET /api/vehiculos?vin=XYZ` - Buscar por VIN
- `GET /api/vehiculos` - Lista paginada con filtros opcionales
- Cada vehículo debe mostrar: placa, marca modelo año color, nombre del cliente, nombre del taller, estatus activo
- Acceso: roles con VEHICULOS_CONSULTAR (Admin + Recepcionista + Cliente propio)

### E. VEHÍCULO - ACTUALIZAR
- Actualizar datos del vehículo (marca, modelo, año, versión, color, kilometraje, fotos)
- Cambiar de cliente o taller cuando cambia ubicación
- **VALIDACIÓN**: No permitir actualizar vehículo desactivado (error 409 "El vehículo está desactivado. Reactívalo antes...")
- Al actualizar: PUT 200 con mensaje "Datos del vehículo actualizados correctamente"
- Si cambia de cliente o taller → crear entrada en HistorialVehiculo
- Acceso: Admin + Recepcionista (VEHICULOS_EDITAR)

### F. VEHÍCULO - DESACTIVAR/REACTIVAR
- `PATCH /api/vehiculos/{id}/desactivar` - requiere motivo ≥5 chars
- `PATCH /api/vehiculos/{id}/reactivar` - verifica que el cliente propietario esté activo
- Al desactivar: set activo=false, guardar motivo, fecha, usuario; crear historial
- Al reactivar: verificar cliente activo, set activo=true, limpiar motivo/fechas; crear historial
- Acceso: Admin + Recepcionista (VEHICULOS_DESACTIVAR)

### G. TALLERES - ALTA
- Formulario: nombre (obligatorio), descripción?, dirección, contacto, teléfono, correo, CP (validar con estado), horarios
- Al guardar: verificar que no exista taller con mismo nombre (error 409)
- Acceso: Admin solo

### H. TALLERES - ASOCIAR EN ALTA DE CLIENTE
- En formulario de alta de cliente: combo buscable de talleres activos
- Filtro en vivo mientras escribes
- Lista con casillas (clic en fila o en casilla para marcar/desmarcar)
- Contador en vivo: 'N taller(es) seleccionado(s)'
- Mínimo 1 taller obligatorio
- Al enviar: si el cliente existe → asociar talleres y mostrar mensaje "Se reutilizó el cliente existente por correo y se asoció a los talleres seleccionados." Si es nuevo cliente → INSERT 201
- Si validación falla → toast rojo + scroll automático al primer campo en rojo
- Acceso: Admin + Recepcionista (CLIENTES_REGISTRAR)

### H. USUARIOS - ADMINISTRACIÓN
- Listado de usuarios con: email, nombre rol, último login, estado
- Botón "Editar" permisos por rol
- Acceso: Admin solo (ROLES_CREAR, ROLES_ASIGNAR)

### I. CITAS
- Lista de citas con: fecha, hora, cliente, vehículo, taller, descripción, estado
- Botón "Nueva cita"
- **La leyenda de estados ya fue eliminada** (decisión 20)
- Acceso: roles correspondientes

### j. DASHBOARD
- Solo: 'Hola, [nombre] [rol]' + insignia de rol
- 0 tarjetas de indicadores
- 0 tarjetas de acceso
- Menú lateral fijo: Clientes, Talleres, Vehículos, Historial, Usuarios, Roles
- Acceso: por rol correspondiente

## PATRONES Y CONVENCIONES YA DEFINIDOS

1. **Validación en frontend**: Usar RX_MARCA/RX_MODELO patrones, componentes Campo con prop `error` y `htmlFor`, validación antes de submit
2. **Validación en backend**: Usar javax.validation.constraints en VehiculoRequest/ClienteRequest, lanzar NegocioException con HttpStatus.CONFLICT (409) o BAD_REQUEST (400)
3. **Manejo de errores**: Siempre devolver estructura con `message` y opcionalmente `errores` mapa por campo
4. **Soft-delete NUNCA borrar físicamente**: Usar flag `activo` y campos `motivoDesactivacion`, `desactivadoEn`, `desactivadoPor`
5. **Nombres de autoridad**: `ROLE_<CLAVE>` en Spring Security (ej: `ROLE_ADMINISTRADOR_DE_TALLER`)
6. **Endpoints duales**: `/api/codigos-postales/{cp}` y alias `/api/sepomex/cp/{cp}`
7. **Documentación protegida**: `ROLE_ADMINISTRADOR_DE_TALLER` en `/documentacion`
8. **Fotos**: Campo String que almacena rutas separadas por coma, max 500 chars
9. **Combo buscable**: Input de filtro arriba + lista con scroll + casillas + contador + close con Escape/clic fuera
10. **Dashboard admin**: Solo saludo + insignia, sin tarjetas de números ni accesos rápidos
11. **Flujo recuperación**: Campo telephone recovery eliminado de registro.html, backend tolera null
12. **Fechas**: Formato `fechaCorta` en frontend, `Instant` en backend

## SEGURIDAD Y PERMISOS

- Login: campo `email` (no username)
- CSRF token en todos los POST/PUT/PATCH DELETE
- Sesión expira al reiniciar contenedor
- Roles basados en authorities `ROLE_<CLAVE>`
- Admin registra talleres; Admin + Recepcionista registran clientes
- Admin puede asignar roles; AdminController rechaza asignar suspendidos
- Recepcionista puede editar/desactivar clientes (actualizado en app.properties)

## ARCHIVOS CLAVE PARA MODIFICAR

1. `backend/src/main/java/mx/tallermecanico/app/clientes/ClienteController.java` - Endpoints REST
2. `backend/src/main/java/mx/tallermecanico/app/clientes/ClienteService.java` - Lógica de negocio (bloquea duplicados)
3. `backend/src/main/java/mx/tallermecanico/app/clientes/ClienteRepository.java` - Queries JPQL
4. `backend/src/main/java/mx/tallermecanico/app/vehiculos/VehiculoController.java` - Endpoints vehículo
5. `backend/src/main/java/mx/tallermecanico/app/vehiculos/VehiculoService.java` - Normalización VIN/Placa
6. `backend/src/main/java/mx/tallermecanico/app/vehiculos/VehiculoRequest.java` - Validaciones vehículo
7. `backend/src/main/java/mx/tallermecanico/app/vehiculos/DatosVehiculo.java` - Record DTO
8. `backend/src/main/java/mx/tallermecanico/app/vehiculos/Vehiculo.java` - Entidad JPA
9. `backend/src/main/java/mx/tallermecanico/app/common/Texto.java` - Utilidades de texto (placa, VIN, normalizar)
10. `frontend/src/pages/Clientes.jsx` - UI administración completa
11. `frontend/src/pages/Dashboard.jsx` - Dashboard admin (solo saludo)
12. `frontend/src/pages/vehiculos/Vehiculos.jsx` - UI vehículo (alta/consultar/actualizar)
13. `frontend/src/pages/clientes/Clientes.jsx` - Ya está implementado
14. `application.properties` - Permisos por rol (añadir CLIENTES_EDITAR/DESACTIVAR a Recepcionista)
15. `Dockerfile` - Build multi-etapa
16. `compose.yaml` - Servicios Docker

## BASE DE DATO INICIAL (YA EXISTE)
- 2 usuarios: admin@taller.local (ADMINISTRADOR_DE_TALLER) + ac7654281@gmail.com Anthony Castillo (CLIENTE)
- 8 roles en BD
- 2 clientes: Ana Torres #1, Luis Gomez #2
- 1 vehículo: ABC-1234 (cliente Anthony Castillo)
- 2 talleres: id=2 "taller norte taller mecanico el rayo" completo/activo; id=1 heredado incompleto
- 0 citas
- taller_cliente: 0 registros

## RESTRICTIONS Y LIMITATIONS
- **NUNCA** crear datos de prueba en BD con el script de inserción
- **NUNCA** usar filtros `docker ps -aq --filter ancestor=...` amplios (borró taller-app antes)
- **NUNCA** ejecutar dos builds concurrentes (conflicto nombre contenedor)
- Builds Docker atascados por DNS intermitente de Docker Desktop - usar parche de jar
- Codificación Windows: no usar `Get-Content`/`Set-Content` de PowerShell en plantillas UTF-8
- Usar .NET `File.ReadAllText/WriteAllText` con `UTF8Encoding($false)`; verificar por bytes (FFFD=`EF BF BD`)

## ENTRADAS SALIDAS EXPECTADAS

### Entradas del Prompt
- Ninguna (sistema ya tiene estructura base)
- Opcional: qué módulo priorizar primero

### Salidas Esperadas del Codex
- Controladores REST completos con annotations @RestController, @RequestMapping, @PreAuthorize
- Servicios con lógica de negocio, validaciones, excepciones personalizadas
- Entidades JPA con anotaciones @Entity, @Table, @Column, relaciones ManyToOne/ManyToMany
- Repositorios JpaRepository con queries personalizadas
- DTOs y Records para request/response
- Validaciones con javax.validation.constraints
- Manejadores de excepciones global
- Frontend componentes React con hooks de estado
- Páginas y routes para todas las vistas
- Dockerfile optimizado
- application.properties con seguridad roles

## EJEMPLO DE COMO DEBE SER UN ENDPOINT GENERADO

```java
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VEHICULOS_CONSULTAR')")
    public ResponseEntity<List<VehiculoDTO>> consultar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Vehiculo> pageData = service.consultar(q, incluirInactivos, page, size);
        return ResponseEntity.ok(pageData.getContent().stream()
                .map(VehiculoDTO::new)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VEHICULOS_REGISTRAR')")
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody VehiculoRequest r) {
        Vehiculo v = service.registrar(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Vehículo con placa " + v.getPlaca() + " registrado correctamente.",
                "vehiculo", VehiculoDTO.vista(v)
        ));
    }
}
```

---
## PARA COPIAR AL PORTAPAPELES / INI
```
[El prompt de arriba]

Copiar y pegar en Codex con el comando /INIT o el equivalente en tu herramienta de IA code generation.
```