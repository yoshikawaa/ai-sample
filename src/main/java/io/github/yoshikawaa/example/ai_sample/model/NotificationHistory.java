package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知履歴エンティティ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

    private Long id;
    private String recipientEmail;
    private NotificationType notificationType;
    private String subject;
    private String body;
    private Status status;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    /**
     * 通知種別
     */
    public static enum NotificationType {
        PASSWORD_RESET,              // パスワードリセットリンク送信
        ACCOUNT_LOCK,                // アカウントロック通知
        ACCOUNT_UNLOCK,              // アカウントアンロックリンク送信
        PASSWORD_RESET_COMPLETE,     // パスワードリセット完了通知
        ACCOUNT_UNLOCK_COMPLETE      // アカウントアンロック完了通知
    }

    /**
     * 送信ステータス
     */
    public static enum Status {
        SUCCESS,   // 送信成功
        FAILURE    // 送信失敗
    }
}
