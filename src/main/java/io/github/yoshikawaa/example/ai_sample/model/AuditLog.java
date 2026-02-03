package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    private String performedBy;
    private String targetEmail;
    private ActionType actionType;
    private String actionDetail;
    private LocalDateTime actionTime;
    private String ipAddress;

    /**
     * アクション種別（CREATE, UPDATE, DELETE, PASSWORD_RESET, ACCOUNT_LOCK, ACCOUNT_UNLOCK）
     */
    public static enum ActionType {
        CREATE, UPDATE, DELETE, PASSWORD_RESET, ACCOUNT_LOCK, ACCOUNT_UNLOCK
    }
}
