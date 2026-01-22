package io.github.yoshikawaa.example.ai_sample.exception;

/**
 * 無効または期限切れのトークンが使用された場合の例外
 */
public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super("無効または期限切れのトークンです。");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
