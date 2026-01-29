package io.github.yoshikawaa.example.ai_sample.service;


import io.github.yoshikawaa.example.ai_sample.model.AccountUnlockToken;
import io.github.yoshikawaa.example.ai_sample.repository.AccountUnlockTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest
@DisplayName("AccountLockService のテスト")
class AccountLockServiceTest {

    @Autowired
    private AccountLockService accountLockService;

    @MockitoBean
    private AccountUnlockTokenRepository accountUnlockTokenRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("アンロックリクエスト時にトークンが生成・保存され、メール送信される")
    void testRequestUnlock() {
        String email = "test@example.com";
        String name = "テストユーザー";
        doNothing().when(emailService).sendEmail(eq(email), anyString(), anyString());
        doNothing().when(accountUnlockTokenRepository).insert(any(AccountUnlockToken.class));

        doNothing().when(notificationService).sendUnlockRequestNotification(eq(email), eq(name), anyString(), anyLong());

        accountLockService.requestUnlock(email, name);

        verify(accountUnlockTokenRepository, times(1)).insert(any(AccountUnlockToken.class));
        verify(notificationService, times(1)).sendUnlockRequestNotification(eq(email), eq(name), anyString(), anyLong());
    }

    @Test
    @DisplayName("アンロック時にトークンが削除される")
    void testUnlockAccount() {
        String email = "test@example.com";
        String token = "unlock-token";
        AccountUnlockToken unlockToken = new AccountUnlockToken(email, token, System.currentTimeMillis() + 10000);
        when(accountUnlockTokenRepository.findByToken(token)).thenReturn(unlockToken);

        boolean result = accountLockService.unlockAccount(token);

        assertThat(result).isTrue();
        verify(accountUnlockTokenRepository, times(1)).deleteByToken(token);
    }

    @Test
    @DisplayName("無効なトークンでアンロックできない")
    void testUnlockAccountWithInvalidToken() {
        String token = "invalid-token";
        when(accountUnlockTokenRepository.findByToken(token)).thenReturn(null);

            boolean result = accountLockService.unlockAccount(token);

        assertThat(result).isFalse();
        verify(accountUnlockTokenRepository, never()).deleteByToken(anyString());
    }

    @Test
    @DisplayName("有効期限切れトークンでアンロックできない")
    void testUnlockAccountWithExpiredToken() {
        String email = "test@example.com";
        String token = "expired-token";
        // 過去時刻
        AccountUnlockToken expiredToken = new AccountUnlockToken(email, token, System.currentTimeMillis() - 10000);
        when(accountUnlockTokenRepository.findByToken(token)).thenReturn(expiredToken);

        boolean result = accountLockService.unlockAccount(token);

        assertThat(result).isFalse();
        verify(accountUnlockTokenRepository, never()).deleteByToken(anyString());
    }
}
