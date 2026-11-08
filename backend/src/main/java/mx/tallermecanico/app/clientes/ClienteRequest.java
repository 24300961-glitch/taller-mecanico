package mx.tallermecanico.app.clientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^\\s*[\\p{L}][\\p{L} .'-]{1,79}\\s*$", message = "El nombre debe tener de 2 a 80 letras (se permiten espacios, puntos, apóstrofos y guiones)")
        String nombre,

        @NotBlank(message = "El apellido paterno es obligatorio")
        @Pattern(regexp = "^\\s*[\\p{L}][\\p{L} .'-]{1,79}\\s*$", message = "El apellido paterno debe tener de 2 a 80 letras (se permiten espacios, puntos, apóstrofos y guiones)")
        String apellidoPaterno,

        @Pattern(regexp = "^$|^\\s*[\\p{L}][\\p{L} .'-]{1,79}\\s*$", message = "El apellido materno debe tener de 2 a 80 letras (se permiten espacios, puntos, apóstrofos y guiones)")
        String apellidoMaterno,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9 ()+-]{10,20}$", message = "El teléfono solo puede contener números, espacios, paréntesis o guiones")
        String telefono,

        @Pattern(regexp = "^$|^[0-9 ()+-]{10,20}$", message = "El teléfono alterno solo puede contener números, espacios, paréntesis o guiones")
        String telefonoAlterno,

        @Pattern(regexp = "^$|^[0-9 ()+-]{10,20}$", message = "El teléfono personal solo puede contener números, espacios, paréntesis o guiones")
        String telefonoPersonal,

        @Pattern(regexp = "^$|^[0-9 ()+-]{10,20}$", message = "El teléfono de trabajo solo puede contener números, espacios, paréntesis o guiones")
        String telefonoTrabajo,

        @Size(max = 160, message = "El nombre del contacto no debe exceder 160 caracteres")
        @Pattern(regexp = "^$|^\\s*[\\p{L}][\\p{L} .'-]{1,159}\\s*$", message = "El nombre del contacto solo admite letras, espacios, puntos, apóstrofos y guiones")
        String nombreContacto,

        @Pattern(regexp = "^$|^[0-9 ()+-]{10,20}$", message = "El teléfono del contacto solo puede contener números, espacios, paréntesis o guiones")
        String telefonoContacto,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Ingresa un correo electrónico válido")
        @Size(max = 190, message = "El correo no debe exceder 190 caracteres")
        String email,

        @Pattern(regexp = "^$|^\\s*(?i)[A-ZÑ&]{3,4}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[A-Z0-9]{3}\\s*$", message = "El RFC no tiene un formato válido (12 o 13 caracteres con fecha real)")
        String rfc,

        @Size(max = 250, message = "La dirección no debe exceder 250 caracteres")
        String direccion,

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 160, message = "La calle no debe exceder 160 caracteres")
        String calle,

        @NotBlank(message = "El número es obligatorio")
        @Size(max = 40, message = "El número no debe exceder 40 caracteres")
        String numero,

        @NotBlank(message = "La colonia es obligatoria")
        @Size(max = 120, message = "La colonia no debe exceder 120 caracteres")
        String colonia,

        @NotBlank(message = "El código postal es obligatorio")
        @Pattern(regexp = "^\\d{5}$", message = "El código postal debe tener 5 dígitos")
        String codigoPostal,

        @NotBlank(message = "El municipio es obligatorio")
        @Size(max = 120, message = "El municipio no debe exceder 120 caracteres")
        String municipio,

        @NotBlank(message = "El estado es obligatorio")
        @Size(max = 120, message = "El estado no debe exceder 120 caracteres")
        String estado,

        boolean confirmarCoincidencias,

        List<Long> tallerIds) {}
