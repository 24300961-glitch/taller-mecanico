# Estructura del backend — Spring Boot

Mapa real de `backend/src/main/java/mx/tallermecanico/app/`. Si el código cambia, este documento se actualiza.

## Capas

```
HTTP → Controller → Service → Repository → Base de datos
                     ↓
                    DTO
```

- **Controller**: valida y delega. Sin reglas de negocio.
- **Service**: reglas, transacciones y normalización.
- **Repository**: consultas.
- **DTO**: entrada/salida de la API.
- **Strategy**: variantes de comportamiento por rol.

## Paquetes

| Paquete | Responsabilidad | Clases principales |
| --- | --- | --- |
| `auth` | Cuentas, login, recuperación de contraseña | `AuthController`, `AuthService`, `UserAccount`, `UserRepository`, `PasswordResetToken` |
| `auth/strategy` | Estrategias de autenticación y recuperación | `StrategyAuthenticationProvider`, `EstrategiaPasswordPorRol`, `EmailRecoveryStrategy`, `SmsRecoveryStrategy` |
| `seguridad` | Roles, permisos y sesión | `Rol`, `Permiso`, `RolService`, `SesionService`, `SeguridadProperties` |
| `usuarios/strategy` | Creación de usuarios según rol | `UserCreationService`, `CreacionUsuarioStrategy` |
| `clientes` | Alta y consulta de clientes | `Cliente`, `ClienteController`, `ClienteService`, `ClienteRequest`, `DatosCliente` |
| `vehiculos` | Alta, consulta e historial de vehículos | `Vehiculo`, `VehiculoController`, `VehiculoService`, `VehiculoRequest`, `HistorialVehiculo` |
| `taller` | Talleres y fotos | `Taller`, `TallerController`, `TallerService`, `FotoController`, `FotoTallerStorage` |
| `dashboard` | Panel por rol | `DashboardApiController` + `dashboard/strategy/*` |
| `sepomex` | Estados y códigos postales | `SepomexController`, `CodigoPostalController`, `SepomexCsvSeeder` |
| `audit` | Bitácora de cambios | `AuditLog`, `AuditService`, `AuditLogRepository` |
| `error` | Manejo central de errores | `ApiExceptionHandler`, `NegocioException`, `CustomErrorController` |
| `config` | Seguridad, datos iniciales, auditoría JPA | `SecurityConfig`, `DataLoader`, `InicializadorSistema`, `JpaAuditConfig` |
| `common` | Utilidades compartidas | `Texto` |
| `web` | Vistas y modelo base | `WebViewController`, `VistaModelAdvice` |
| `citas` | **Sin implementación** (vacío) | — |

## Patrón Strategy

Se usa en tres lugares:

| Dominio | Interfaz | Implementaciones |
| --- | --- | --- |
| Autenticación | `AutenticacionStrategy` | Email, SMS |
| Dashboard | `DashboardStrategy` | Una por rol |
| Creación de usuario | `CreacionUsuarioStrategy` | Una por rol |

Regla: agregar un rol nuevo = agregar su estrategia, no un `if` nuevo.

## Recursos

| Archivo | Uso |
| --- | --- |
| `application.properties` | Configuración por variables de entorno (`DB_*`, `MAIL_*`, `TALLERES_STORAGE`) |
| `db/migration/` | Migraciones Flyway |
| `static/` | Build del frontend para despliegue de un solo origen |

## Pruebas

- `backend/src/test/` no existe en el repositorio actual.
- Los tests se crean con el agente `qa` y no tocan la base de datos real.
