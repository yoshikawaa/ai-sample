package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.config.AccountUnlockProperties;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@DisplayName("NotificationService のテスト")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private NotificationHistoryService notificationHistoryService;

    @MockitoBean
    private AccountUnlockProperties accountUnlockProperties;

    // ========================================
    // アカウントロック関連
    // ========================================

    @Test
    @DisplayName("sendAccountLockedNotification: アカウントロック通知の送信が送信される")
    void testSendAccountLockedNotification() {
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setName("テストユーザー");
        when(accountUnlockProperties.getHostUrl()).thenReturn("http://localhost");
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        notificationService.sendAccountLockedNotification(customer);

        verify(emailService, times(1)).sendEmail(eq("test@example.com"), anyString(), anyString());
        verify(notificationHistoryService, times(1)).recordNotification(
            eq("test@example.com"),
            eq(NotificationHistory.NotificationType.ACCOUNT_LOCK),
            anyString(),
            anyString(),
            eq(true),
            eq(null)
        );
    }

    @Test
    @DisplayName("sendAccountLockedNotification: アカウントロック通知の送信に失敗した場合、通知履歴に失敗が記録される")
    void testSendAccountLockedNotification_EmailFailure() {
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setName("テストユーザー");
        when(accountUnlockProperties.getHostUrl()).thenReturn("http://localhost");
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(false);

        notificationService.sendAccountLockedNotification(customer);

        verify(emailService, times(1)).sendEmail(eq("test@example.com"), anyString(), anyString());
        verify(notificationHistoryService, times(1)).recordNotification(
            eq("test@example.com"),
            eq(NotificationHistory.NotificationType.ACCOUNT_LOCK),
            anyString(),
            anyString(),
            eq(false),
            anyString()
        );
    }

    @Test
    @DisplayName("sendUnlockRequestNotification: アカウントロック解除リクエスト通知が送信される")
    void testSendUnlockRequestNotification() {
        String email = "test@example.com";
        String name = "テストユーザー";
        String unlockLink = "http://localhost/unlock?token=xyz789";
        long expiryMinutes = 30;
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        notificationService.sendUnlockRequestNotification(email, name, unlockLink, expiryMinutes);

        verify(emailService, times(1)).sendEmail(eq(email), anyString(), anyString());
        verify(notificationHistoryService, times(1)).recordNotification(
            eq(email),
            eq(NotificationHistory.NotificationType.ACCOUNT_UNLOCK),
            anyString(),
            anyString(),
            eq(true),
            eq(null)
        );
    }

    @Test
    @DisplayName("sendAccountUnlockComplete: アカウントロック解除完了通知が送信される")
    void testSendAccountUnlockComplete() {
        String email = "test@example.com";
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        notificationService.sendAccountUnlockComplete(email);

        verify(emailService, times(1)).sendEmail(eq(email), anyString(), anyString());
        verify(notificationHistoryService, times(1)).recordNotification(
            eq(email),
            eq(NotificationHistory.NotificationType.ACCOUNT_UNLOCK_COMPLETE),
            anyString(),
            anyString(),
            eq(true),
            eq(null)
        );
    }

    // ========================================
    // パスワードリセット関連
    // ========================================

    @Test
    @DisplayName("sendPasswordResetLink: パスワードリセットリンクが送信される")
    void testSendPasswordResetLink() {
        String email = "test@example.com";
        String resetLink = "http://localhost:8080/password-reset/confirm?token=abc123";
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        notificationService.sendPasswordResetLink(email, resetLink);

        verify(emailService, times(1)).sendEmail(eq(email), anyString(), anyString());
        verify(notificationHistoryService, times(1)).recordNotification(
            eq(email),
            eq(NotificationHistory.NotificationType.PASSWORD_RESET),
            anyString(),
            anyString(),
            eq(true),
            eq(null)
        );
    }

    @Test
    @DisplayName("sendPasswordResetComplete: パスワードリセット完了通知が送信される")
    void testSendPasswordResetComplete() {
        String email = "test@example.com";
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        notificationService.sendPasswordResetComplete(email);

        verify(emailService, times(1)).sendEmail(eq(email), anyString(), anyString());
        verify(notificationHistoryService, times(1)).recordNotification(
            eq(email),
            eq(NotificationHistory.NotificationType.PASSWORD_RESET_COMPLETE),
            anyString(),
            anyString(),
            eq(true),
            eq(null)
        );
    }
}
