package io.github.yoshikawaa.example.ai_sample.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomerNotFoundException のテスト
 */
@DisplayName("CustomerNotFoundException のテスト")
class CustomerNotFoundExceptionTest {

    @Test
    @DisplayName("メールアドレス付きコンストラクタで適切なメッセージを持つ例外を生成できる")
    void testConstructorWithEmail() {
        // Given
        String email = "test@example.com";

        // When
        CustomerNotFoundException exception = new CustomerNotFoundException(email);

        // Then
        assertThat(exception.getMessage()).isEqualTo("顧客が見つかりません。メールアドレス: test@example.com");
    }

    @Test
    @DisplayName("メッセージと原因付きコンストラクタで例外を生成できる")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "カスタムエラーメッセージ";
        Throwable cause = new RuntimeException("原因の例外");

        // When
        CustomerNotFoundException exception = new CustomerNotFoundException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
