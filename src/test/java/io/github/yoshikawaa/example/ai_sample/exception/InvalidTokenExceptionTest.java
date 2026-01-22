package io.github.yoshikawaa.example.ai_sample.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InvalidTokenException のテスト
 */
@DisplayName("InvalidTokenException のテスト")
class InvalidTokenExceptionTest {

    @Test
    @DisplayName("デフォルトコンストラクタでデフォルトメッセージを持つ例外を生成できる")
    void testDefaultConstructor() {
        // When
        InvalidTokenException exception = new InvalidTokenException();

        // Then
        assertThat(exception.getMessage()).isEqualTo("無効または期限切れのトークンです。");
    }

    @Test
    @DisplayName("カスタムメッセージ付きコンストラクタで例外を生成できる")
    void testConstructorWithCustomMessage() {
        // Given
        String customMessage = "カスタムエラーメッセージ";

        // When
        InvalidTokenException exception = new InvalidTokenException(customMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}
