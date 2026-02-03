package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {
    private Long id;
    private String email;
    private LocalDateTime loginTime;
    private Status status;
    private String ipAddress;
    private String userAgent;
    private String failureReason;

    /**
     * ログインステータス（SUCCESS, FAILURE, LOCKED, LOGOUT）
     */
    public static enum Status {
        SUCCESS, FAILURE, LOCKED, LOGOUT
    }
}
