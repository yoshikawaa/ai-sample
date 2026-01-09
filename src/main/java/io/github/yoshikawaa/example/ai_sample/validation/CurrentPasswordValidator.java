package io.github.yoshikawaa.example.ai_sample.validation;

import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class CurrentPasswordValidator implements ConstraintValidator<CurrentPassword, String> {

    private final PasswordEncoder passwordEncoder;

    public CurrentPasswordValidator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 空の値は有効とする（@NotBlank などでチェックするため）
        if (value == null || value.isEmpty()) {
            return true;
        }

        // 現在の認証情報を取得
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomerUserDetails userDetails) {
            String currentPasswordHash = userDetails.getPassword();
            return passwordEncoder.matches(value, currentPasswordHash);
        }

        return false; // 認証情報が取得できない場合は無効
    }
}
