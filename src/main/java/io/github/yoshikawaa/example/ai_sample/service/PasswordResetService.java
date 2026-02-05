package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.exception.InvalidTokenException;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.PasswordResetToken;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.repository.PasswordResetTokenRepository;
import io.github.yoshikawaa.example.ai_sample.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PasswordResetService {

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;

    public void sendResetLink(@NonNull String email) {
        // セキュリティ: メールアドレスの存在有無を外部に漏らさない
        var customerOpt = customerRepository.findByEmail(email);
        if (customerOpt.isEmpty()) {
            log.warn("パスワードリセット試行: 存在しないメールアドレス {}", email);
            return; // 成功と同じ動作（セキュリティ対策）
        }

        String token = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + 3600000; // 1時間後に期限切れ

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(email);
        resetToken.setResetToken(token);
        resetToken.setTokenExpiry(expiry);
        passwordResetTokenRepository.insert(resetToken);

        String resetLink = "http://localhost:8080/password-reset/confirm?token=" + token;
        notificationService.sendPasswordResetLink(email, resetLink);

        // 監査ログを記録
        auditLogService.recordAudit(email, email, AuditLog.ActionType.PASSWORD_RESET, "パスワードリセットリンク送信", RequestContextUtil.getClientIpAddress());
    }

    @Transactional(readOnly = true)
    public void validateResetToken(String token) {
        getValidatedToken(token);
    }

    public void updatePassword(String token, String newPassword) {
        PasswordResetToken resetToken = getValidatedToken(token);
        String email = resetToken.getEmail();

        // パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(newPassword);

        // ハッシュ化されたパスワードを保存
        customerRepository.updatePassword(email, hashedPassword);

        // パスワードリセット完了通知を送信
        notificationService.sendPasswordResetComplete(email);

        // 監査ログを記録
        auditLogService.recordAudit(email, email, AuditLog.ActionType.PASSWORD_RESET, "パスワードリセット完了", RequestContextUtil.getClientIpAddress());
        
        // ログイン試行記録・ロック状態をリセット
        loginAttemptService.resetAttempts(email);

        // トークンを削除
        passwordResetTokenRepository.deleteByEmail(email);
    }

    /**
     * トークンを検証し、有効なPasswordResetTokenを返す
     * 
     * @param token リセットトークン
     * @return 有効なPasswordResetToken
     * @throws InvalidTokenException トークンが無効または期限切れの場合
     */
    private PasswordResetToken getValidatedToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByResetToken(token);
        if (resetToken == null || resetToken.getTokenExpiry() < System.currentTimeMillis()) {
            throw new InvalidTokenException();
        }
        return resetToken;
    }
}