package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.AccountUnlockToken;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.repository.AccountUnlockTokenRepository;
import io.github.yoshikawaa.example.ai_sample.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import io.github.yoshikawaa.example.ai_sample.config.AccountUnlockProperties;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountLockService {

    private final AccountUnlockTokenRepository tokenRepository;
    private final AccountUnlockProperties accountUnlockProperties;
    private final LoginAttemptService loginAttemptService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * ロック解除申請を受付・トークン発行・メール送信（文面生成はNotificationServiceで担当）
     */
    @Transactional
    public void requestUnlock(String email, String name) {
        String token = UUID.randomUUID().toString();
        long expiry = Instant.now().plusSeconds(accountUnlockProperties.getTokenExpirySeconds()).toEpochMilli();
        tokenRepository.insert(new AccountUnlockToken(email, token, expiry));

        String unlockLink = accountUnlockProperties.getHostUrl() + "/account-unlock?token=" + token;
        long expiryMinutes = accountUnlockProperties.getTokenExpirySeconds() / 60;
        notificationService.sendUnlockRequestNotification(email, name, unlockLink, expiryMinutes);
        
        // 監査ログを記録
        auditLogService.recordAudit(email, email, AuditLog.ActionType.ACCOUNT_LOCK, "アカウントロック解除申請", RequestContextUtil.getClientIpAddress());
    }

    /**
     * トークン検証・ロック解除・メール送信（文面生成はNotificationServiceで担当）
     */
    @Transactional
    public boolean unlockAccount(String token) {
        AccountUnlockToken unlockToken = tokenRepository.findByToken(token);
        if (unlockToken == null || unlockToken.getTokenExpiry() < System.currentTimeMillis()) {
            return false;
        }
        // ロック解除処理
        loginAttemptService.resetAttempts(unlockToken.getEmail());
        tokenRepository.deleteByToken(token);
        
        notificationService.sendAccountUnlockComplete(unlockToken.getEmail());
        
        // 監査ログを記録
        auditLogService.recordAudit(unlockToken.getEmail(), unlockToken.getEmail(), AuditLog.ActionType.ACCOUNT_UNLOCK, 
            "アカウントロック解除完了", RequestContextUtil.getClientIpAddress());
        
        log.info("アカウントロック解除: email={}", unlockToken.getEmail());
        return true;
    }
}
