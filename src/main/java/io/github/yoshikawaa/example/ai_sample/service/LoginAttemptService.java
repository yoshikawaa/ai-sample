
package io.github.yoshikawaa.example.ai_sample.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


import io.github.yoshikawaa.example.ai_sample.config.LoginAttemptProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.yoshikawaa.example.ai_sample.model.LoginAttempt;
import io.github.yoshikawaa.example.ai_sample.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginAttemptProperties loginAttemptProperties;
    private final CustomerService customerService;
    private final NotificationService notificationService;

    /**
     * ログイン失敗回数を記録し、ロック閾値に達した場合はアカウントをロックしてtrueを返す。
     * （トランザクション内で即時ロック判定が可能）
     */
    public boolean handleFailedLoginAttempt(String email) {
        var loginAttemptOpt = loginAttemptRepository.findByEmail(email);
        long currentTime = System.currentTimeMillis();

        if (loginAttemptOpt.isEmpty()) {
            // 初回失敗
            LoginAttempt loginAttempt = new LoginAttempt();
            loginAttempt.setEmail(email);
            loginAttempt.setAttemptCount(1);
            loginAttempt.setLastAttemptTime(currentTime);
            // 1回目でロックはしない
            loginAttempt.setLockedUntil(null);
            loginAttemptRepository.insert(loginAttempt);
            log.info("ログイン失敗記録: email={}, attemptCount=1", email);
            return false;
        } else {
            LoginAttempt loginAttempt = loginAttemptOpt.get();
            int newAttemptCount = loginAttempt.getAttemptCount() + 1;
            loginAttempt.setAttemptCount(newAttemptCount);
            loginAttempt.setLastAttemptTime(currentTime);

            if (newAttemptCount >= loginAttemptProperties.getMax()) {
                // ロック
                var customer = customerService.getCustomerByEmail(email);
                notificationService.sendAccountLockedNotification(customer);
                long lockedUntil = currentTime + loginAttemptProperties.getLockDurationMs();
                loginAttempt.setLockedUntil(lockedUntil);
                loginAttemptRepository.update(loginAttempt);
                String lockedUntilStr = formatTimestamp(lockedUntil);
                log.warn("アカウントロック: email={}, lockedUntil={}", email, lockedUntilStr);
                return true;
            } else {
                // ロック解除
                loginAttempt.setLockedUntil(null);
                loginAttemptRepository.update(loginAttempt);
                log.info("ログイン失敗記録: email={}, attemptCount={}", email, newAttemptCount);
                return false;
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String email) {
        Long lockedUntil = extractLockedUntil(email);
        if (lockedUntil == null) {
            return false;
        }
        return System.currentTimeMillis() < lockedUntil;
    }

    @Transactional(readOnly = true)
    public String getLockedUntilFormatted(String email) {
        Long lockedUntil = extractLockedUntil(email);
        if (lockedUntil == null) {
            return null;
        }
        return formatTimestamp(lockedUntil);
    }

    public void resetAttempts(String email) {
        log.info("ログイン試行回数リセット: email={}", email);
        loginAttemptRepository.deleteByEmail(email);
    }

    /**
     * ログイン試行記録からlockedUntil値を安全に取得
     */
    private Long extractLockedUntil(String email) {
        return loginAttemptRepository.findByEmail(email)
            .map(LoginAttempt::getLockedUntil)
            .orElse(null);
    }

    private String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }
}
