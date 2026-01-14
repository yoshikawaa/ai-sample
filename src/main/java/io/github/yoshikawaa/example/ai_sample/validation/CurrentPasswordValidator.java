package io.github.yoshikawaa.example.ai_sample.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return false; // 認証情報が取得できない場合は無効
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomerUserDetails userDetails) {
            String currentPasswordHash = userDetails.getPassword();
            return passwordEncoder.matches(value, currentPasswordHash);
        }

        return false; // 認証情報が CustomerUserDetails でない場合は無効
    }
}