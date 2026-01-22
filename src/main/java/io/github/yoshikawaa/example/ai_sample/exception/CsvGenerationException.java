package io.github.yoshikawaa.example.ai_sample.exception;

/**
 * CSV生成失敗時の例外（システムエラー）
 */
public class CsvGenerationException extends RuntimeException {

    public CsvGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CsvGenerationException() {
        super("CSV生成中にエラーが発生しました。");
    }
}
