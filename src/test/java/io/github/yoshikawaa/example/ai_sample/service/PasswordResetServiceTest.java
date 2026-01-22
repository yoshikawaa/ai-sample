package io.github.yoshikawaa.example.ai_sample.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.exception.InvalidTokenException;
import io.github.yoshikawaa.example.ai_sample.model.PasswordResetToken;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("PasswordResetService のテスト")
class PasswordResetServiceTest {

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
    private CustomerRepository customerRepository;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setEmail("test@example.com");
        testCustomer.setPassword("oldHashedPassword");
        testCustomer.setName("Test User");
    }

    @Test
    @DisplayName("sendResetLink: メールアドレスが存在する場合、トークンを生成してメールを送信する")
    void testSendResetLink_正常系() {
        // Arrange
        when(customerRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testCustomer));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // Act
        passwordResetService.sendResetLink("test@example.com");

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).insert(tokenCaptor.capture());
        
        PasswordResetToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedToken.getResetToken()).isNotNull().matches("[a-f0-9-]{36}"); // UUID形式
        assertThat(capturedToken.getTokenExpiry()).isGreaterThan(System.currentTimeMillis());

        verify(emailService).sendEmail(
                eq("test@example.com"),
                eq("パスワードリセット"),
                contains("http://localhost:8080/password-reset/confirm?token=")
        );
    }

    @Test
    @DisplayName("sendResetLink: メールアドレスが存在しない場合、何もせず正常終了する（セキュリティ対策）")
    void testSendResetLink_メールアドレスが存在しない() {
        // Arrange
        when(customerRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert - 例外をスローせず正常終了
        assertDoesNotThrow(() -> passwordResetService.sendResetLink("nonexistent@example.com"));

        // トークン生成やメール送信は行われない
        verify(passwordResetTokenRepository, never()).insert(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendResetLink: トークンの有効期限が約1時間後に設定される")
    void testSendResetLink_有効期限の確認() {
        // Arrange
        when(customerRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testCustomer));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        long beforeTime = System.currentTimeMillis() + 3600000 - 1000; // 許容誤差1秒
        
        // Act
        passwordResetService.sendResetLink("test@example.com");

        long afterTime = System.currentTimeMillis() + 3600000 + 1000; // 許容誤差1秒

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).insert(tokenCaptor.capture());
        
        PasswordResetToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getTokenExpiry())
                .isBetween(beforeTime, afterTime);
    }

    @Test
    @DisplayName("validateResetToken: 有効なトークンの場合、例外をスローしない")
    void testValidateResetToken_正常系() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setEmail("test@example.com");
        validToken.setResetToken("valid-token");
        validToken.setTokenExpiry(System.currentTimeMillis() + 3600000); // 1時間後

        when(passwordResetTokenRepository.findByResetToken("valid-token"))
                .thenReturn(validToken);

        // Act & Assert
        assertDoesNotThrow(() -> passwordResetService.validateResetToken("valid-token"));
        verify(passwordResetTokenRepository).findByResetToken("valid-token");
    }

    @Test
    @DisplayName("validateResetToken: 存在しないトークンの場合、InvalidTokenExceptionをスローする")
    void testValidateResetToken_存在しないトークン() {
        // Arrange
        when(passwordResetTokenRepository.findByResetToken("nonexistent-token"))
                .thenReturn(null);

        // Act & Assert
        assertThrows(InvalidTokenException.class, 
            () -> passwordResetService.validateResetToken("nonexistent-token"));
    }

    @Test
    @DisplayName("validateResetToken: 有効期限切れのトークンの場合、InvalidTokenExceptionをスローする")
    void testValidateResetToken_有効期限切れ() {
        // Arrange
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setEmail("test@example.com");
        expiredToken.setResetToken("expired-token");
        expiredToken.setTokenExpiry(System.currentTimeMillis() - 1000); // 1秒前に期限切れ

        when(passwordResetTokenRepository.findByResetToken("expired-token"))
                .thenReturn(expiredToken);

        // Act & Assert
        assertThrows(InvalidTokenException.class, 
            () -> passwordResetService.validateResetToken("expired-token"));
    }

    @Test
    @DisplayName("updatePassword: 有効なトークンの場合、パスワードを更新しトークンを削除する")
    void testUpdatePassword_正常系() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setEmail("test@example.com");
        validToken.setResetToken("valid-token");
        validToken.setTokenExpiry(System.currentTimeMillis() + 3600000);

        when(passwordResetTokenRepository.findByResetToken("valid-token"))
                .thenReturn(validToken);
        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("hashedNewPassword");
        doNothing().when(customerRepository).updatePassword(anyString(), anyString());
        doNothing().when(passwordResetTokenRepository).deleteByEmail(anyString());

        // Act
        passwordResetService.updatePassword("valid-token", "newPassword123");

        // Assert
        verify(passwordEncoder).encode("newPassword123");
        verify(customerRepository).updatePassword("test@example.com", "hashedNewPassword");
        verify(passwordResetTokenRepository).deleteByEmail("test@example.com");
    }

    @Test
    @DisplayName("updatePassword: 存在しないトークンの場合、例外をスローする")
    void testUpdatePassword_存在しないトークン() {
        // Arrange
        when(passwordResetTokenRepository.findByResetToken("nonexistent-token"))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> passwordResetService.updatePassword("nonexistent-token", "newPassword123"))
                .isInstanceOf(InvalidTokenException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(customerRepository, never()).updatePassword(anyString(), anyString());
        verify(passwordResetTokenRepository, never()).deleteByEmail(anyString());
    }

    @Test
    @DisplayName("updatePassword: 有効期限切れのトークンの場合、例外をスローする")
    void testUpdatePassword_有効期限切れのトークン() {
        // Arrange
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setEmail("test@example.com");
        expiredToken.setResetToken("expired-token");
        expiredToken.setTokenExpiry(System.currentTimeMillis() - 1000);

        when(passwordResetTokenRepository.findByResetToken("expired-token"))
                .thenReturn(expiredToken);

        // Act & Assert
        assertThatThrownBy(() -> passwordResetService.updatePassword("expired-token", "newPassword123"))
                .isInstanceOf(InvalidTokenException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(customerRepository, never()).updatePassword(anyString(), anyString());
        verify(passwordResetTokenRepository, never()).deleteByEmail(anyString());
    }

    @Test
    @DisplayName("updatePassword: パスワードエンコーダーを使用してパスワードをハッシュ化する")
    void testUpdatePassword_パスワードハッシュ化の確認() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setEmail("test@example.com");
        validToken.setResetToken("valid-token");
        validToken.setTokenExpiry(System.currentTimeMillis() + 3600000);

        when(passwordResetTokenRepository.findByResetToken("valid-token"))
                .thenReturn(validToken);
        when(passwordEncoder.encode("rawPassword"))
                .thenReturn("encodedPassword123");
        doNothing().when(customerRepository).updatePassword(anyString(), anyString());
        doNothing().when(passwordResetTokenRepository).deleteByEmail(anyString());

        // Act
        passwordResetService.updatePassword("valid-token", "rawPassword");

        // Assert
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(customerRepository).updatePassword(emailCaptor.capture(), passwordCaptor.capture());
        
        assertThat(emailCaptor.getValue()).isEqualTo("test@example.com");
        assertThat(passwordCaptor.getValue()).isEqualTo("encodedPassword123");
    }

    @Test
    @DisplayName("updatePassword: パスワード更新後にトークンが削除される")
    void testUpdatePassword_トークン削除の確認() {
        // Arrange
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setEmail("test@example.com");
        validToken.setResetToken("valid-token");
        validToken.setTokenExpiry(System.currentTimeMillis() + 3600000);

        when(passwordResetTokenRepository.findByResetToken("valid-token"))
                .thenReturn(validToken);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashedPassword");
        doNothing().when(customerRepository).updatePassword(anyString(), anyString());
        doNothing().when(passwordResetTokenRepository).deleteByEmail(anyString());

        // Act
        passwordResetService.updatePassword("valid-token", "newPassword123");

        // Assert
        verify(passwordResetTokenRepository).deleteByEmail("test@example.com");
    }
}
