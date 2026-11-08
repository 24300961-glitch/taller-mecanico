package mx.tallermecanico.app.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContrasenaSegura.Validador.class)
public @interface ContrasenaSegura {

    String message() default "La contraseña debe tener entre 12 y 72 caracteres e incluir mayúscula, minúscula, número y símbolo, sin espacios al inicio o final";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<ContrasenaSegura, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext ctx) {
            if (valor == null || valor.isEmpty()) return true;
            if (valor.length() < 12 || valor.getBytes(StandardCharsets.UTF_8).length > 72) return false;
            if (!valor.equals(valor.strip())) return false;
            return valor.chars().anyMatch(Character::isUpperCase)
                    && valor.chars().anyMatch(Character::isLowerCase)
                    && valor.chars().anyMatch(Character::isDigit)
                    && valor.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        }
    }
}
