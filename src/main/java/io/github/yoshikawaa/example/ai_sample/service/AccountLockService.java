package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.AccountUnlockToken;

import io.github.yoshikawaa.example.ai_sample.repository.AccountUnlockTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import io.github.yoshikawaa.example.ai_sample.config.AccountUnlockProperties;

@Slf4j
@Service
public class AccountLockService {

    private final AccountUnlockTokenRepository tokenRepository;
    private final AccountUnlockProperties accountUnlockProperties;
    private final LoginAttemptService loginAttemptService;
    private final NotificationService notificationService;

    public AccountLockService(
            AccountUnlockTokenRepository tokenRepository,
            AccountUnlockProperties accountUnlockProperties,
            LoginAttemptService loginAttemptService,
            NotificationService notificationService) {
        this.tokenRepository = tokenRepository;
        this.accountUnlockProperties = accountUnlockProperties;
        this.loginAttemptService = loginAttemptService;
        this.notificationService = notificationService;
    }



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
    }

    /**
     * トークン検証・ロック解除
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
        log.info("アカウントロック解除: email={}", unlockToken.getEmail());
        return true;
    }
}
