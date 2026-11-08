package mx.tallermecanico.app.taller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos recibidos al registrar o editar un taller. */
public record TallerRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120, message = "El nombre no debe exceder 120 caracteres") String nombre,
        @NotBlank(message = "El RFC es obligatorio") @Pattern(regexp = "(?i)^[A-ZÑ&]{3,4}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[A-Z0-9]{3}$", message = "El RFC debe tener 12 o 13 caracteres válidos con fecha real") String rfc,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Ingresa un correo electrónico válido") @Size(max = 190, message = "El correo no debe exceder 190 caracteres") String email,
        @NotBlank(message = "El teléfono es obligatorio") @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres") @Pattern(regexp = "^[0-9 ()+.-]+$", message = "El teléfono contiene caracteres no permitidos") String telefono,
        @NotBlank(message = "La calle es obligatoria") @Size(max = 160, message = "La calle no debe exceder 160 caracteres") String calle,
        @NotBlank(message = "El número es obligatorio") @Size(max = 40, message = "El número no debe exceder 40 caracteres") String numero,
        @NotBlank(message = "La colonia es obligatoria") @Size(max = 120, message = "La colonia no debe exceder 120 caracteres") String colonia,
        @NotBlank(message = "El código postal es obligatorio") @Pattern(regexp = "^\\d{5}$", message = "El código postal debe tener 5 dígitos") String codigoPostal,
        @NotBlank(message = "El municipio es obligatorio") @Size(max = 120, message = "El municipio no debe exceder 120 caracteres") String municipio,
        @NotBlank(message = "El estado es obligatorio") @Size(max = 120, message = "El estado no debe exceder 120 caracteres") String estado,
        @NotNull(message = "Selecciona si el taller estará activo") Boolean activo) { }
