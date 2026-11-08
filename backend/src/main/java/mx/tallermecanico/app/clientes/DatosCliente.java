package mx.tallermecanico.app.clientes;

public record DatosCliente(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String telefono,
        String telefonoAlterno,
        String telefonoPersonal,
        String telefonoTrabajo,
        String nombreContacto,
        String telefonoContacto,
        String email,
        String rfc,
        String direccion,
        String calle,
        String numero,
        String colonia,
        String codigoPostal,
        String municipio,
        String estado) {}
