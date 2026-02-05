package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * アクティビティタイムライン統合モデル
 * 顧客の活動履歴を統合的に表示するための共通モデル
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTimeline {
    
    private Long id;
    private LocalDateTime timestamp;
    private ActivityType activityType;
    private String description;
    private String detail;
    private String ipAddress;
    private String status;
    
    /**
     * アクティビティ種別
     */
    public static enum ActivityType {
        LOGIN_SUCCESS("ログイン成功"),
        LOGIN_FAILURE("ログイン失敗"),
        LOGOUT("ログアウト"),
        SESSION_EXCEEDED("セッション超過"),
        INFO_UPDATED("情報変更"),
        PASSWORD_CHANGED("パスワード変更"),
        PASSWORD_RESET("パスワードリセット"),
        ACCOUNT_LOCKED("アカウントロック"),
        ACCOUNT_UNLOCKED("アカウントアンロック"),
        ACCOUNT_CREATED("アカウント作成"),
        ACCOUNT_DELETED("アカウント削除"),
        NOTIFICATION_SENT("通知送信");
        
        private final String displayName;
        
        ActivityType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
