# Tabla: citas

## Descripción
Tabla de citas o appointments para el taller mecánico. Permite agendar citas para servicios, reparaciones o consultas. Cada cita está asociada a un taller y opcionalmente a un cliente y vehículo.

## Columnas

| Campo | Tipo | Nulo | Predeterminado | Comentario |
|---|---|---|---|---|
| id | bigint | NO | null | Identificador único |
| cliente_id | bigint | YES | null | FK a tabla clientes (opcional; cita puede ser sin cliente registrado) |
| taller_id | bigint | NO | null | FK a talleres (obligatorio; toda cita debe tener taller asignado) |
| vehiculo_id | bigint | YES | null | FK a vehiculales (opcional; cita puede ser genérica sin vehículo específico) |
| servicio | varchar(100) | NO | null | Nombre del servicio solicitado (ej. "Revisión general", "Cambio de aceite") |
| descripcion | text | YES | null | Descripción opcional del problema o requisito |
| fecha_hora | datetime | NO | null | Fecha y hora programada de la cita |
| estatus | varchar(20) | NO | 'pendiente' | Estado de la cita: 'pendiente', 'confirmada', 'completada', 'cancelada' |
| creado_at | datetime | NO | null | Fecha de creación de la solicitud |
| updated_at | datetime | NO | null | Fecha de última actualización |

## Valores de `estatus`
- `pendiente`: Cita agendada, esperando confirmación
- `confirmada`: Cliente confirmó su asistencia
- `completada`: Servicio finalizado
- `cancelada`: Cita cancelada por cliente o taller

## Relaciones
- **FK**: `cliente_id` → `clientes.id` (ManyToOne, opcional)
- **FK**: `taller_id` → `talleres.id` (ManyToOne, obligatorio)
- **FK**: `vehiculo_id` → `vehiculos.id` (ManyToOne, opcional)

## Índices
- **Index**: `taller_id + fecha_hora` (para consultas de citas por taller y rango de fechas)
- **Index**: `estatus` (para filtrar citas por estado)
- **Index**: `cliente_id` (para listado de citas por cliente)

## Notas de negocio
- Toda cita debe tener un taller asignado (validación en el controlador/servicio).
- El cliente y el vehículo son opcionales; se pueden crear citas genéricas para talleres.
- La fecha_hora debe ser futura respecto al momento de creación; el frontend valida esto con `TN.requisitosContrasena` patterns adaptados.
- El admin y recepcionista pueden ver todas las citas; el cliente solo las suyas (filtrado por email de la cuenta autenticada).

## Notas de implementación
- El dashboard de citas usa strategy por rol para filtrado (`DashboardStrategyService` inyecta `permisos` en el modelo).
- `citas.html` muestra el menú por permisos y mensajes de error reales del backend.
- Las estrategias de dashboard NO se extienden a citas (tal como dice la decisión tomada: "No se creó Strategy nueva para ... citas").