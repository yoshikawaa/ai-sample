package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.config.AccountUnlockProperties;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@DisplayName("NotificationService のテスト")
class NotificationServiceTest {

    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private AccountUnlockProperties accountUnlockProperties;

    @Test
    @DisplayName("アカウントロック通知メールが送信される")
    void testSendAccountLockedNotification() {
        NotificationService notificationService = new NotificationService(emailService, accountUnlockProperties);
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setName("テストユーザー");
        // hostUrlのモック
        org.mockito.Mockito.when(accountUnlockProperties.getHostUrl()).thenReturn("http://localhost");

        notificationService.sendAccountLockedNotification(customer);

        verify(emailService, times(1)).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("アンロック申請通知メールが送信される")
    void testSendUnlockRequestNotification() {
        NotificationService notificationService = new NotificationService(emailService, accountUnlockProperties);
        String email = "test@example.com";
        String name = "テストユーザー";
        String unlockLink = "http://localhost/account-unlock?token=abc";
        long expiryMinutes = 30L;

        notificationService.sendUnlockRequestNotification(email, name, unlockLink, expiryMinutes);

        verify(emailService, times(1)).sendEmail(eq(email), anyString(), anyString());
    }
}
