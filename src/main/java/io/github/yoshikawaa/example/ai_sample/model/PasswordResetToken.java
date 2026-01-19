package io.github.yoshikawaa.example.ai_sample.model;

import lombok.Data;

@Data
public class PasswordResetToken {
    private String email; // Customer テーブルの主キーと一致
    private String resetToken;
    private Long tokenExpiry;
}
