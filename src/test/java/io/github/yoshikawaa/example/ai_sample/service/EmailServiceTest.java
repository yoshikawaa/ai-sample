package io.github.yoshikawaa.example.ai_sample.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.github.yoshikawaa.example.ai_sample.exception.EmailSendException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("EmailService のテスト")
class EmailServiceTest {

    @TestConfiguration
    static class TestGreenMailConfig {
        @Bean(initMethod = "start", destroyMethod = "stop")
        @Primary
        public GreenMail testGreenMail() {
            ServerSetup serverSetup = ServerSetup.SMTP.dynamicPort();
            serverSetup.setServerStartupTimeout(10000);
            serverSetup.setVerbose(false);
            return new GreenMail(serverSetup);
        }
    }

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: 正常にメールを送信できる")
    void testSendEmail_正常系() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("test@example.com", "テストタイトル", "テスト本文");

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: 宛先、件名、本文が正しく設定される")
    void testSendEmail_メール内容の確認() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("recipient@example.com", "重要な件名", "メール本文です");

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: HTMLメールとして送信される（setText の第2引数が true）")
    void testSendEmail_HTML形式の確認() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("test@example.com", "HTMLメール", "<h1>HTML本文</h1>");

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: MailException が発生した場合、EmailSendException をスローする")
    void testSendEmail_MailException発生時() {
        // Arrange
        doThrow(new MailSendException("SMTP接続エラー"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendEmail("test@example.com", "件名", "本文"))
                .isInstanceOf(EmailSendException.class)
                .hasMessage("メール送信中にエラーが発生しました")
                .hasCauseInstanceOf(MailException.class);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: 複数回呼び出しても正常に動作する")
    void testSendEmail_複数回送信() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("user1@example.com", "件名1", "本文1");
        emailService.sendEmail("user2@example.com", "件名2", "本文2");
        emailService.sendEmail("user3@example.com", "件名3", "本文3");

        // Assert
        verify(mailSender, times(3)).createMimeMessage();
        verify(mailSender, times(3)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: 空文字列でも正常に処理される")
    void testSendEmail_空文字列() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("test@example.com", "", "");

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: 長い本文でも正常に処理される")
    void testSendEmail_長い本文() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));
        String longBody = "あ".repeat(10000); // 10000文字の本文

        // Act
        emailService.sendEmail("test@example.com", "長い本文テスト", longBody);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: 特殊文字を含むメールアドレスでも処理される")
    void testSendEmail_特殊文字を含むメールアドレス() {
        // Arrange
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendEmail("user+test@example.co.jp", "件名", "本文");

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail: createMimeMessage で null が返された場合でもエラーハンドリングされる")
    void testSendEmail_MimeMessage作成失敗() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendEmail("test@example.com", "件名", "本文"))
                .isInstanceOf(Exception.class); // NullPointerException または IllegalStateException

        verify(mailSender).createMimeMessage();
    }
}
