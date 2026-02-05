
package io.github.yoshikawaa.example.ai_sample.service;

import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.github.yoshikawaa.example.ai_sample.model.LoginAttempt;
import io.github.yoshikawaa.example.ai_sample.repository.LoginAttemptRepository;
import io.github.yoshikawaa.example.ai_sample.model.Customer;

@DisplayName("LoginAttemptService のテスト")
class LoginAttemptServiceTest {

    @Nested
    @SpringBootTest
    @DisplayName("デフォルトプロパティでの動作検証")
    class DefaultTest {

        @MockitoBean
        private CustomerService customerService;

        @MockitoBean
        private LoginAttemptRepository loginAttemptRepository;

        @MockitoBean
        private EmailService emailService;

        @MockitoBean
        private NotificationHistoryService notificationHistoryService;

        @Autowired
        private LoginAttemptService loginAttemptService;

        @BeforeEach
        void setUpEmailService() {
            // EmailServiceのメール送信を抑止（booleanを返すメソッドなのでwhenを使用）
            when(emailService.sendEmail(any(), any(), any())).thenReturn(true);
        }

        @Test
        @DisplayName("handleFailedLoginAttempt: 初回失敗でログイン試行を記録できる")
        void testHandleFailedLoginAttempt_FirstTime() {
            // Given
            String email = "test@example.com";
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            loginAttemptService.handleFailedLoginAttempt(email);

            // Then
            verify(loginAttemptRepository, times(1)).insert(any(LoginAttempt.class));
        }

        @Test
        @DisplayName("handleFailedLoginAttempt: 2回目以降の失敗でカウントを増やす")
        void testHandleFailedLoginAttempt_Increment() {
            // Given
            String email = "test@example.com";
            LoginAttempt existingAttempt = new LoginAttempt();
            existingAttempt.setEmail(email);
            existingAttempt.setAttemptCount(2);
            existingAttempt.setLockedUntil(null);
            existingAttempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(existingAttempt));

            // When
            loginAttemptService.handleFailedLoginAttempt(email);

            // Then
            verify(loginAttemptRepository, times(1)).update(any(LoginAttempt.class));
        }

        @Test
        @DisplayName("handleFailedLoginAttempt: 5回目の失敗でアカウントロックする")
        void testHandleFailedLoginAttempt_Lock() {
            // Given
            String email = "test@example.com";
            LoginAttempt existingAttempt = new LoginAttempt();
            existingAttempt.setEmail(email);
            existingAttempt.setAttemptCount(4);
            existingAttempt.setLockedUntil(null);
            existingAttempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(existingAttempt));

            // 顧客情報のスタブ
            Customer mockCustomer = new Customer();
            mockCustomer.setEmail(email);
            mockCustomer.setName("Test User");
            when(customerService.getCustomerByEmail(email)).thenReturn(mockCustomer);

            // When
            loginAttemptService.handleFailedLoginAttempt(email);

            // Then
            verify(loginAttemptRepository, times(1)).update(any(LoginAttempt.class));
        }

        @Test
        @DisplayName("isLocked: ロック中の場合trueを返す")
        void testIsLocked_Locked() {
            // Given
            String email = "test@example.com";
            LoginAttempt lockedAttempt = new LoginAttempt();
            lockedAttempt.setEmail(email);
            lockedAttempt.setAttemptCount(5);
            lockedAttempt.setLockedUntil(System.currentTimeMillis() + 1000000); // 未来の時刻
            lockedAttempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(lockedAttempt));

            // When
            boolean isLocked = loginAttemptService.isLocked(email);

            // Then
            assertThat(isLocked).isTrue();
        }

        @Test
        @DisplayName("isLocked: ロック期間が過ぎた場合falseを返す")
        void testIsLocked_Expired() {
            // Given
            String email = "test@example.com";
            LoginAttempt expiredAttempt = new LoginAttempt();
            expiredAttempt.setEmail(email);
            expiredAttempt.setAttemptCount(5);
            expiredAttempt.setLockedUntil(System.currentTimeMillis() - 1000); // 過去の時刻
            expiredAttempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(expiredAttempt));

            // When
            boolean isLocked = loginAttemptService.isLocked(email);

            // Then
            assertThat(isLocked).isFalse();
        }


        @Test
        @DisplayName("isLocked: lockedUntilがnullの場合falseを返す")
        void testIsLocked_LockedUntilNull() {
            // Given
            String email = "test@example.com";
            LoginAttempt attempt = new LoginAttempt();
            attempt.setEmail(email);
            attempt.setAttemptCount(3);
            attempt.setLockedUntil(null);
            attempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(attempt));

            // When
            boolean isLocked = loginAttemptService.isLocked(email);

            // Then
            assertThat(isLocked).isFalse();
        }

        @Test
        @DisplayName("resetAttempts: ログイン試行記録を削除する")
        void testResetAttempts() {
            // Given
            String email = "test@example.com";

            // When
            loginAttemptService.resetAttempts(email);

            // Then
            verify(loginAttemptRepository, times(1)).deleteByEmail(email);
        }


        @Test
        @DisplayName("getLockedUntilFormatted: lockedUntilがnullの場合nullを返す")
        void testGetLockedUntilFormatted_LockedUntilNull() {
            // Given
            String email = "test@example.com";
            LoginAttempt attempt = new LoginAttempt();
            attempt.setEmail(email);
            attempt.setAttemptCount(3);
            attempt.setLockedUntil(null);
            attempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(attempt));

            // When
            String formattedTime = loginAttemptService.getLockedUntilFormatted(email);

            // Then
            assertThat(formattedTime).isNull();
        }

        @Test
        @DisplayName("getLockedUntilFormatted: lockedUntilが設定されている場合、日付文字列を返す")
        void testGetLockedUntilFormatted_ReturnsFormattedString() {
            // Given
            String email = "test@example.com";
            long lockedUntil = System.currentTimeMillis() + 3600_000; // 1時間後
            LoginAttempt attempt = new LoginAttempt();
            attempt.setEmail(email);
            attempt.setAttemptCount(3);
            attempt.setLockedUntil(lockedUntil);
            attempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(attempt));

            // When
            String formattedTime = loginAttemptService.getLockedUntilFormatted(email);

            // Then
            assertThat(formattedTime).isNotNull();
            // yyyy-MM-dd HH:mm 形式
            assertThat(formattedTime).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
        }

        @Test
        @DisplayName("lockAccountByAdmin: 既存レコードがない場合、新規レコードを作成してロックする")
        void testLockAccountByAdmin_NewRecord() {
            // Given
            String email = "newuser@example.com";
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            loginAttemptService.lockAccountByAdmin(email);

            // Then
            verify(loginAttemptRepository, times(1)).insert(any(LoginAttempt.class));
            verify(loginAttemptRepository, times(0)).update(any(LoginAttempt.class));
        }

        @Test
        @DisplayName("lockAccountByAdmin: 既存レコードがある場合、更新してロックする")
        void testLockAccountByAdmin_ExistingRecord() {
            // Given
            String email = "existinguser@example.com";
            LoginAttempt existingAttempt = new LoginAttempt();
            existingAttempt.setEmail(email);
            existingAttempt.setAttemptCount(2);
            existingAttempt.setLockedUntil(null);
            existingAttempt.setLastAttemptTime(System.currentTimeMillis());
            when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(existingAttempt));

            // When
            loginAttemptService.lockAccountByAdmin(email);

            // Then
            verify(loginAttemptRepository, times(0)).insert(any(LoginAttempt.class));
            verify(loginAttemptRepository, times(1)).update(any(LoginAttempt.class));
        }

        @Test
        @DisplayName("unlockAccountByAdmin: アカウントロックを解除する")
        void testUnlockAccountByAdmin() {
            // Given
            String email = "lockeduser@example.com";

            // When
            loginAttemptService.unlockAccountByAdmin(email);

            // Then
            verify(loginAttemptRepository, times(1)).deleteByEmail(email);
        }

    }

    @Nested
    @SpringBootTest(properties = {
        "app.security.login.attempt.max=3",
        "app.security.login.attempt.lock-duration-ms=60000"
    })
    @DisplayName("プロパティ変更時の動作検証")
    class LoginAttemptPropertiesChangeTest {


        @MockitoBean
        private CustomerService customerService;

        @MockitoBean
        private LoginAttemptRepository loginAttemptRepository;

        @MockitoBean
        private EmailService emailService;

        @MockitoBean
        private NotificationHistoryService notificationHistoryService;

        @Autowired
        private LoginAttemptService loginAttemptService;

        @BeforeEach
        void setUpEmailService() {
            // EmailServiceのメール送信を抑止（booleanを返すメソッドなのでwhenを使用）
            when(emailService.sendEmail(any(), any(), any())).thenReturn(true);
        }

        @Test
        @DisplayName("max値を変更した場合、その回数でロックされることを検証する")
        void testHandleFailedLoginAttempt_LockThresholdChange() {
            String email = "lock3@example.com";
            final LoginAttempt[] currentAttempt = {null};
            // findByEmailの挙動を模擬
            when(loginAttemptRepository.findByEmail(email)).thenAnswer(invocation -> {
                if (currentAttempt[0] == null) {
                    return Optional.empty();
                }
                return Optional.of(currentAttempt[0]);
            });
            // insertの挙動を模擬
            doAnswer(invocation -> {
                LoginAttempt inserted = invocation.getArgument(0);
                currentAttempt[0] = inserted;
                return null;
            }).when(loginAttemptRepository).insert(any(LoginAttempt.class));
            // updateの挙動を模擬
            org.mockito.Mockito.doAnswer(invocation -> {
                LoginAttempt updated = invocation.getArgument(0);
                currentAttempt[0].setAttemptCount(updated.getAttemptCount());
                currentAttempt[0].setLastAttemptTime(updated.getLastAttemptTime());
                currentAttempt[0].setLockedUntil(updated.getLockedUntil());
                return null;
            }).when(loginAttemptRepository).update(any(LoginAttempt.class));

            // 顧客情報のスタブ
            Customer mockCustomer = new Customer();
            mockCustomer.setEmail(email);
            mockCustomer.setName("Lock3 User");
            when(customerService.getCustomerByEmail(email)).thenReturn(mockCustomer);

            // 1回目失敗（まだロックされない）
            boolean locked1 = loginAttemptService.handleFailedLoginAttempt(email);
            assertThat(locked1).isFalse();
            assertThat(currentAttempt[0].getAttemptCount()).isEqualTo(1);
            assertThat(currentAttempt[0].getLockedUntil()).isNull();

            // 2回目失敗（まだロックされない）
            boolean locked2 = loginAttemptService.handleFailedLoginAttempt(email);
            assertThat(locked2).isFalse();
            assertThat(currentAttempt[0].getAttemptCount()).isEqualTo(2);
            assertThat(currentAttempt[0].getLockedUntil()).isNull();

            // 3回目失敗（ロックされる）
            boolean locked3 = loginAttemptService.handleFailedLoginAttempt(email);
            assertThat(locked3).isTrue();
            assertThat(currentAttempt[0].getAttemptCount()).isEqualTo(3);
            assertThat(currentAttempt[0].getLockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("lock-duration-msを変更した場合、ロック時間が反映されることを検証する")
        void testHandleFailedLoginAttempt_LockDurationChange() {
            String email = "lockduration@example.com";
            final LoginAttempt[] currentAttempt = {null};
            // findByEmailの挙動を模擬
            when(loginAttemptRepository.findByEmail(email)).thenAnswer(invocation -> {
                if (currentAttempt[0] == null) {
                    return Optional.empty();
                }
                return Optional.of(currentAttempt[0]);
            });
            // insertの挙動を模擬
            org.mockito.Mockito.doAnswer(invocation -> {
                LoginAttempt inserted = invocation.getArgument(0);
                currentAttempt[0] = inserted;
                return null;
            }).when(loginAttemptRepository).insert(any(LoginAttempt.class));
            // updateの挙動を模擬
            org.mockito.Mockito.doAnswer(invocation -> {
                LoginAttempt updated = invocation.getArgument(0);
                currentAttempt[0].setAttemptCount(updated.getAttemptCount());
                currentAttempt[0].setLastAttemptTime(updated.getLastAttemptTime());
                currentAttempt[0].setLockedUntil(updated.getLockedUntil());
                return null;
            }).when(loginAttemptRepository).update(any(LoginAttempt.class));

            // 顧客情報のスタブ
            Customer mockCustomer = new Customer();
            mockCustomer.setEmail(email);
            mockCustomer.setName("LockDuration User");
            when(customerService.getCustomerByEmail(email)).thenReturn(mockCustomer);

            // 1回目失敗（まだロックされない）
            loginAttemptService.handleFailedLoginAttempt(email);
            // 2回目失敗（まだロックされない）
            loginAttemptService.handleFailedLoginAttempt(email);
            // 3回目失敗（ロックされる）
            long before = System.currentTimeMillis();
            boolean locked = loginAttemptService.handleFailedLoginAttempt(email);
            long after = System.currentTimeMillis();
            assertThat(locked).isTrue();
            assertThat(currentAttempt[0].getLockedUntil()).isNotNull();
            // 設定値（60000ms）±100ms程度の誤差で検証
            long expected = 60000L;
            long actual = currentAttempt[0].getLockedUntil() - before;
            assertThat(actual).isBetween(expected - 100, expected + (after - before) + 100);
        }
    }
}
