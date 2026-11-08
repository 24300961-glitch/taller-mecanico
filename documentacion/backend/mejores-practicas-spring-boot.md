# Mejores prácticas — Spring Boot (Taller Mecánico)

Reglas de calidad del backend. Sirven para revisar código y para que el `documentador-backend` sepa qué reflejar.

## 1. Organización

- **Por funcionalidad, no por capa global.** Cada paquete agrupa su controlador, servicio, repositorio y DTO:
  `clientes/`, `vehiculos/`, `talleres/`, `auth/`, `usuarios/`, `dashboard/`, `sepomex/`, `audit/`.
- Un paquete nuevo solo si aporta un dominio real. No crear paquetes vacíos.
- `config/` solo configuración transversal (seguridad, beans, filtros).
- `common/` solo utilidades compartidas de verdad.

## 2. Capas

| Capa | Regla |
| --- | --- |
| `@RestController` | Recibe, valida y delega. Sin lógica de negocio. |
| `@Service` | Reglas de negocio y transacciones. |
| `@Repository` | Acceso a datos. Sin reglas de negocio. |
| DTO | Entrada y salida de la API. Nunca exponer entidades. |

## 3. DTOs y validación

- Un DTO de entrada por operación (`ClienteRequest`, `VehiculoRequest`).
- Validar con Jakarta Validation: `@NotBlank`, `@Size`, `@Pattern`, `@Email`, `@Min`.
- La validación de formato vive en el DTO; la de negocio en el servicio.
- Normalizar en el servicio (mayúsculas, guiones, trim), no en el controlador.

## 4. Errores

- Usar un `@RestControllerAdvice` central (`error/`).
- Respuestas consistentes: código HTTP + mensaje claro + ruta.
- `400` datos inválidos, `401` sin sesión, `403` sin permiso, `404` no existe, `409` conflicto (duplicados, estado inválido).
- No devolver trazas ni detalles internos al cliente.

## 5. Transacciones y datos

- `@Transactional` en métodos de servicio que escriben.
- Lecturas simples: sin transacción explícita.
- **Soft delete**: borrar = marcar `activo = false`. No borrar filas con historial.
- Migraciones con Flyway. Nunca cambiar una migración ya aplicada: crear una nueva.
- Índices en columnas de búsqueda y en claves foráneas.

## 6. Seguridad

- Spring Security con sesión; CSRF activo.
- Autorización por rol con `@PreAuthorize` o reglas del `SecurityFilterChain`.
- Contraseñas con `PasswordEncoder` (BCrypt). Nunca en texto plano ni en logs.
- No registrar datos personales ni tokens en logs.
- Sesiones: invalidar al cambiar contraseña o rol.

## 7. Validación de entrada

- VIN: 17 caracteres, sin `I`, `O`, `Q`.
- Placa: formato `ABC-1234` normalizado.
- Email: formato válido y único.
- Fechas: ISO-8601.
- Archivos: validar tipo y tamaño (límite actual 15 MB).

## 8. Rendimiento

- Paginación en el servidor (`Pageable`), nunca devolver listas completas.
- Evitar consultas N+1: usar `JOIN FETCH` o proyecciones cuando aplique.
- Ordenamiento en el servidor, no en el cliente.

## 9. Logs y auditoría

- `INFO` para eventos de negocio, `WARN` para rechazos, `ERROR` para fallos.
- Auditoría de cambios sensibles en el paquete `audit/`.
- Sin datos sensibles en los mensajes.

## 10. Pruebas

- Unitarias de servicios con Mockito.
- Integración de controladores con `MockMvc`.
- Nunca apuntar las pruebas a la base de datos real.
- Un test debe poder correr solo y en cualquier orden.

## 11. Checklist antes de entregar

- [ ] Compila (`mvn -DskipTests compile`).
- [ ] Pasa pruebas (`mvn test`).
- [ ] DTO validado y normalizado.
- [ ] Errores con el código HTTP correcto.
- [ ] Sin lógica de negocio en el controlador.
- [ ] Migración nueva (si cambió el esquema).
- [ ] Sin secretos en el código.
