package io.github.yoshikawaa.example.ai_sample.exception;

/**
 * メール送信失敗時の例外（システムエラー）
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailSendException() {
        super("メール送信中にエラーが発生しました。");
    }
}
