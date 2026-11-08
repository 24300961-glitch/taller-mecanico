package mx.tallermecanico.app.taller;

/** Datos validados y normalizados para persistir un taller. */
public record DatosTaller(String nombre, String rfc, String email, String telefono, String calle,
                          String numero, String colonia, String codigoPostal, String municipio,
                          String estado, boolean activo) { }
