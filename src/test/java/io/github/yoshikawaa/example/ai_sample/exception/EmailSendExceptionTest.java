package io.github.yoshikawaa.example.ai_sample.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmailSendException のテスト
 */
@DisplayName("EmailSendException のテスト")
class EmailSendExceptionTest {

    @Test
    @DisplayName("メッセージと原因付きコンストラクタで例外を生成できる")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "メール送信に失敗しました";
        Throwable cause = new RuntimeException("原因の例外");

        // When
        EmailSendException exception = new EmailSendException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("デフォルトコンストラクタでデフォルトメッセージを持つ例外を生成できる")
    void testDefaultConstructor() {
        // When
        EmailSendException exception = new EmailSendException();

        // Then
        assertThat(exception.getMessage()).isEqualTo("メール送信中にエラーが発生しました。");
    }
}
