package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.config.AccountUnlockProperties;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知メール送信の共通サービス
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final EmailService emailService;
    private final AccountUnlockProperties accountUnlockProperties;

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
        emailService.sendEmail(customer.getEmail(), subject, body);
    }

    /**
     * アカウントロック解除申請メール送信
     */
    public void sendUnlockRequestNotification(String email, String name, String unlockLink, long expiryMinutes) {
        String subject = "【アカウント解除申請】ご案内";
        String body = String.format(
            "%s様\n\nアカウント解除申請を受け付けました。\n\n下記リンクから解除手続きを完了してください。\n\n解除リンク: %s\n\n有効期限: %d分\n\nご不明点はサポートまでご連絡ください。",
            name, unlockLink, expiryMinutes
        );
        emailService.sendEmail(email, subject, body);
        log.info("アカウントロック解除申請メール送信: email={}", email);
        log.debug("アカウント解除リンク: {}", unlockLink);
    }
}
