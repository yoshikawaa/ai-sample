package io.github.yoshikawaa.example.ai_sample.exception;

/**
 * ビジネスロジックエラーを表す基底例外クラス
 * ユーザーに表示可能なエラーメッセージを持つ
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
