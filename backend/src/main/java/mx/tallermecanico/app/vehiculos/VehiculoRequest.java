package mx.tallermecanico.app.vehiculos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehiculoRequest(
        @NotNull(message = "Selecciona el cliente propietario")
        Long clienteId,

        @NotNull(message = "Selecciona el taller donde está el vehículo")
        Long tallerId,

        @NotBlank(message = "La marca es obligatoria")
        @Size(min = 2, max = 40, message = "La marca debe tener entre 2 y 40 caracteres")
        @Pattern(regexp = "^[\\p{L}0-9 .&'-]+$", message = "La marca contiene caracteres no permitidos")
        String marca,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 60, message = "El modelo no debe exceder 60 caracteres")
        @Pattern(regexp = "^[\\p{L}0-9 ./&'-]+$", message = "El modelo contiene caracteres no permitidos")
        String modelo,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 1900, message = "El año debe ser 1900 o posterior")
        @Max(value = 9999, message = "El año debe tener cuatro dígitos")
        Integer anio,

        @Size(max = 30, message = "La versión no debe exceder 30 caracteres")
        String version,

        @NotNull(message = "El kilometraje es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        Integer kilometraje,

        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "(?i)^\\s*[A-Z0-9]{2,4}-[A-Z0-9]{2,4}(-[A-Z0-9]{2,4})?\\s*$",
                message = "La placa debe ir en mayúsculas y separada por un guion, por ejemplo ABC-1234 o ABC-12-34")
        String placa,

        @Pattern(regexp = "(?i)^(?:\\s*[A-HJ-NPR-Z0-9]{17}\\s*)?$", message = "El VIN debe tener 17 caracteres alfanuméricos (sin I, O ni Q)")
        String vin,

        @Size(max = 30, message = "El color no debe exceder 30 caracteres")
        @Pattern(regexp = "^$|^[\\p{L} -]+$", message = "El color solo puede contener letras")
        String color,

        @Size(max = 500, message = "Las fotos no deben exceder 500 caracteres")
        String fotos) {}
