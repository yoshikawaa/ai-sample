package io.github.yoshikawaa.example.ai_sample.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CsvGenerationException のテスト
 */
@DisplayName("CsvGenerationException のテスト")
class CsvGenerationExceptionTest {

    @Test
    @DisplayName("メッセージと原因付きコンストラクタで例外を生成できる")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "CSV生成に失敗しました";
        Throwable cause = new RuntimeException("原因の例外");

        // When
        CsvGenerationException exception = new CsvGenerationException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("デフォルトコンストラクタでデフォルトメッセージを持つ例外を生成できる")
    void testDefaultConstructor() {
        // When
        CsvGenerationException exception = new CsvGenerationException();

        // Then
        assertThat(exception.getMessage()).isEqualTo("CSV生成中にエラーが発生しました。");
    }
}
