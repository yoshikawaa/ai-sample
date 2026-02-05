package io.github.yoshikawaa.example.ai_sample.service;


import io.github.yoshikawaa.example.ai_sample.model.AccountUnlockToken;
import io.github.yoshikawaa.example.ai_sample.repository.AccountUnlockTokenRepository;
import io.github.yoshikawaa.example.ai_sample.repository.AuditLogRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;

@DisplayName("AccountLockService のテスト")
class AccountLockServiceTest {

    @Nested
    @SpringBootTest
    @DisplayName("デフォルトプロパティでの動作検証")
    class DefaultTest {
        @Autowired
        private AccountLockService accountLockService;

        @MockitoBean
        private AccountUnlockTokenRepository accountUnlockTokenRepository;

        @MockitoBean
        private EmailService emailService;

        @MockitoBean
        private NotificationService notificationService;

        @MockitoBean
        private NotificationHistoryService notificationHistoryService;

        @MockitoBean
        private LoginAttemptService loginAttemptService;

        @MockitoBean
        private AuditLogRepository auditLogRepository;

        @BeforeEach
        void setUpAuditLogMock() {
            doNothing().when(auditLogRepository).insert(any());
        }

        @Test
        @DisplayName("アンロックリクエスト時にトークンが生成・保存され、メール送信される")
        void testRequestUnlock() {
            String email = "test@example.com";
            String name = "テストユーザー";
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

        @Test
        @DisplayName("一度利用したアンロックトークンを再利用した場合、アンロックできない")
        void testUnlockAccount_TokenReuseNotAllowed() {
            String email = "test@example.com";
            String token = "unlock-token";
            long expiry = System.currentTimeMillis() + 10000;
            AccountUnlockToken unlockToken = new AccountUnlockToken(email, token, expiry);

            // 1回目: 正常にアンロック
            when(accountUnlockTokenRepository.findByToken(token)).thenReturn(unlockToken);
            doNothing().when(accountUnlockTokenRepository).deleteByToken(token);
            boolean firstResult = accountLockService.unlockAccount(token);
            assertThat(firstResult).isTrue();
            verify(accountUnlockTokenRepository, times(1)).deleteByToken(token);

            // 2回目: トークンは既に削除済み（findByTokenはnullを返す）
            when(accountUnlockTokenRepository.findByToken(token)).thenReturn(null);
            boolean secondResult = accountLockService.unlockAccount(token);
            assertThat(secondResult).isFalse();
            // 削除は1回のみ呼ばれていること
            verify(accountUnlockTokenRepository, times(1)).deleteByToken(token);
        }
    }

    @Nested
    @SpringBootTest(properties = {
        "app.security.account-unlock.token-expiry-seconds=60",
        "app.security.account-unlock.host-url=http://testhost:1234"
    })
    @DisplayName("AccountUnlockProperties変更時の動作検証")
    class AccountUnlockPropertiesChangeTest {
        @Autowired
        private AccountLockService accountLockService;

        @MockitoBean
        private AccountUnlockTokenRepository accountUnlockTokenRepository;

        @MockitoBean
        private NotificationService notificationService;

        @MockitoBean
        private LoginAttemptService loginAttemptService;

        @MockitoBean
        private AuditLogRepository auditLogRepository;

        @BeforeEach
        void setUpAuditLogMock() {
            org.mockito.Mockito.doNothing().when(auditLogRepository).insert(org.mockito.ArgumentMatchers.any());
        }

        @BeforeEach
        void setUp() {
            doNothing().when(accountUnlockTokenRepository).insert(any(AccountUnlockToken.class));
            doNothing().when(notificationService).sendUnlockRequestNotification(anyString(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("token-expiry-secondsの反映を検証する")
        void testTokenExpirySecondsProperty() {
            String email = "test@example.com";
            String name = "プロパティテスト";
            final AccountUnlockToken[] capturedToken = {null};
            doAnswer(invocation -> {
                capturedToken[0] = invocation.getArgument(0);
                return null;
            }).when(accountUnlockTokenRepository).insert(any(AccountUnlockToken.class));

            accountLockService.requestUnlock(email, name);

            assertThat(capturedToken[0]).isNotNull();
            long now = System.currentTimeMillis();
            long expiry = capturedToken[0].getTokenExpiry();
            // 60秒後±2秒程度の誤差で検証
            assertThat(expiry - now).isBetween(58000L, 62000L);
        }

        @Test
        @DisplayName("host-urlの反映とunlockLink生成内容を検証する")
        void testHostUrlPropertyAndUnlockLink() {
            String email = "test2@example.com";
            String name = "ホストURLテスト";
            final String[] capturedUnlockLink = {null};
            doAnswer(invocation -> {
                capturedUnlockLink[0] = invocation.getArgument(2);
                return null;
            }).when(notificationService).sendUnlockRequestNotification(eq(email), eq(name), anyString(), anyLong());

            accountLockService.requestUnlock(email, name);

            assertThat(capturedUnlockLink[0]).isNotNull();
            assertThat(capturedUnlockLink[0]).startsWith("http://testhost:1234/account-unlock?token=");
        }
    }
}
