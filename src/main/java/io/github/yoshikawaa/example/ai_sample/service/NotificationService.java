package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.config.AccountUnlockProperties;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知メール送信の共通サービス
 * すべての通知の文面生成とメール送信を一元管理
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final EmailService emailService;
    private final NotificationHistoryService notificationHistoryService;
    private final AccountUnlockProperties accountUnlockProperties;

    // ========================================
    // アカウントロック関連
    // ========================================

    /**
     * アカウントロック通知メール送信
     */
    public void sendAccountLockedNotification(Customer customer) {
        log.info("アカウントロック通知メール送信: email={}", customer.getEmail());
        String unlockRequestLink = accountUnlockProperties.getHostUrl() + "/account-unlock/request?email=" + customer.getEmail();
        String subject = "【重要】アカウントがロックされました";
        String body = String.format(
            "%s様\n\nセキュリティ保護のため、アカウントが一時的にロックされました。\n\n解除をご希望の場合は以下のリンクから申請してください。\n\n解除申請リンク: %s\n\nご不明点はサポートまでご連絡ください。",
            customer.getName(), unlockRequestLink
        );
        
        sendAndRecordEmail(customer.getEmail(), NotificationHistory.NotificationType.ACCOUNT_LOCK, subject, body);
    }

    /**
     * アカウントロック解除申請メール送信
     */
    public void sendUnlockRequestNotification(String email, String name, String unlockLink, long expiryMinutes) {
        log.info("アカウントロック解除申請メール送信: email={}", email);
        log.debug("アカウント解除リンク: {}", unlockLink);
        
        String subject = "【アカウント解除申請】ご案内";
        String body = String.format(
            "%s様\n\nアカウント解除申請を受け付けました。\n\n下記リンクから解除手続きを完了してください。\n\n解除リンク: %s\n\n有効期限: %d分\n\nご不明点はサポートまでご連絡ください。",
            name, unlockLink, expiryMinutes
        );
        
        sendAndRecordEmail(email, NotificationHistory.NotificationType.ACCOUNT_UNLOCK, subject, body);
    }

    /**
     * アカウントロック解除完了通知メール送信
     */
    public void sendAccountUnlockComplete(String email) {
        log.info("アカウントロック解除完了通知送信: email={}", email);
        
        String subject = "アカウントロック解除完了";
        String body = "アカウントのロックが解除されました。ログインしてください。";
        
        sendAndRecordEmail(email, NotificationHistory.NotificationType.ACCOUNT_UNLOCK_COMPLETE, subject, body);
    }

    // ========================================
    // パスワードリセット関連
    // ========================================

    /**
     * パスワードリセットリンクメール送信
     */
    public void sendPasswordResetLink(String email, String resetLink) {
        log.info("パスワードリセットリンク送信: email={}", email);
        log.debug("リセットリンク：{}", resetLink);
        
        String subject = "パスワードリセット";
        String body = "以下のリンクをクリックしてパスワードをリセットしてください: " + resetLink;
        
        sendAndRecordEmail(email, NotificationHistory.NotificationType.PASSWORD_RESET, subject, body);
    }

    /**
     * パスワードリセット完了通知メール送信
     */
    public void sendPasswordResetComplete(String email) {
        log.info("パスワードリセット完了通知送信: email={}", email);
        
        String subject = "パスワードリセット完了";
        String body = "パスワードのリセットが完了しました。新しいパスワードでログインしてください。";
        
        sendAndRecordEmail(email, NotificationHistory.NotificationType.PASSWORD_RESET_COMPLETE, subject, body);
    }

    /**
     * メール送信と履歴記録を実行（共通処理）
     * 
     * @param email 送信先メールアドレス
     * @param notificationType 通知種別
     * @param subject 件名
     * @param body 本文
     */
    private void sendAndRecordEmail(String email, 
                                     NotificationHistory.NotificationType notificationType,
                                     String subject, 
                                     String body) {
        boolean success = emailService.sendEmail(email, subject, body);
        notificationHistoryService.recordNotification(
            email,
            notificationType,
            subject,
            body,
            success,
            success ? null : "メール送信失敗"
        );
    }
}
