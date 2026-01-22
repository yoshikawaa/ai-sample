package io.github.yoshikawaa.example.ai_sample.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BusinessException のテスト
 */
@DisplayName("BusinessException のテスト")
class BusinessExceptionTest {

    @Test
    @DisplayName("メッセージ付きコンストラクタで例外を生成できる")
    void testConstructorWithMessage() {
        // Given
        String message = "テストエラーメッセージ";

        // When
        BusinessException exception = new BusinessException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("メッセージと原因付きコンストラクタで例外を生成できる")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "テストエラーメッセージ";
        Throwable cause = new RuntimeException("原因の例外");

        // When
        BusinessException exception = new BusinessException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
