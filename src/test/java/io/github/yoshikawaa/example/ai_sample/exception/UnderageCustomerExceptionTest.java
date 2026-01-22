package io.github.yoshikawaa.example.ai_sample.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UnderageCustomerException のテスト
 */
@DisplayName("UnderageCustomerException のテスト")
class UnderageCustomerExceptionTest {

    @Test
    @DisplayName("デフォルトコンストラクタでデフォルトメッセージを持つ例外を生成できる")
    void testDefaultConstructor() {
        // When
        UnderageCustomerException exception = new UnderageCustomerException();

        // Then
        assertThat(exception.getMessage()).isEqualTo("未成年の登録はできません。");
    }

    @Test
    @DisplayName("カスタムメッセージ付きコンストラクタで例外を生成できる")
    void testConstructorWithCustomMessage() {
        // Given
        String customMessage = "カスタムエラーメッセージ";

        // When
        UnderageCustomerException exception = new UnderageCustomerException(customMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}
