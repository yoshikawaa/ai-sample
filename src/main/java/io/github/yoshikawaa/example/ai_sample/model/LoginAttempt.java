package io.github.yoshikawaa.example.ai_sample.model;

import lombok.Data;

@Data
public class LoginAttempt {
    private String email;
    private int attemptCount;
    private Long lockedUntil;
    private Long lastAttemptTime;
}
