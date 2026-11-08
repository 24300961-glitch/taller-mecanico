package mx.tallermecanico.app.seguridad;

public enum Permiso {
    USUARIOS_CONSULTAR("Consultar usuarios"),
    ROLES_CREAR("Crear roles"),
    ROLES_ASIGNAR("Asignar roles a usuarios"),
    CLIENTES_CONSULTAR("Consultar clientes"),
    CLIENTES_REGISTRAR("Registrar clientes"),
    CLIENTES_EDITAR("Editar y corregir clientes"),
    CLIENTES_DESACTIVAR("Desactivar y reactivar clientes"),
    VEHICULOS_CONSULTAR("Consultar vehículos"),
    VEHICULOS_REGISTRAR("Registrar vehículos"),
    VEHICULOS_EDITAR("Editar y corregir vehículos"),
    VEHICULOS_DESACTIVAR("Desactivar y reactivar vehículos");

    private final String descripcion;

    Permiso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
