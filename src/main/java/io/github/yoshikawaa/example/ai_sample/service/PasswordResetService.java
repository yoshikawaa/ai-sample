package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.exception.InvalidTokenException;
import io.github.yoshikawaa.example.ai_sample.model.PasswordResetToken;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.repository.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class PasswordResetService {

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(CustomerRepository customerRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

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
        emailService.sendEmail(email, "パスワードリセット", "以下のリンクをクリックしてパスワードをリセットしてください: " + resetLink);
        log.info("リセットリンク：{}", resetLink);
    }

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