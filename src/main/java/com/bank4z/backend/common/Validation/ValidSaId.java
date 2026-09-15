package com.bank4z.backend.common.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SaIdNumberValidator.class)
public @interface ValidSaId {
    String message() default "Invalid South African ID number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}