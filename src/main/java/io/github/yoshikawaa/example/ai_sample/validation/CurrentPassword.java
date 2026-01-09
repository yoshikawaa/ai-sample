package io.github.yoshikawaa.example.ai_sample.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CurrentPasswordValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentPassword {

    String message() default "現在のパスワードが正しくありません。";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
