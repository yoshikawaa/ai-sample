package io.github.yoshikawaa.example.ai_sample.exception;

/**
 * 顧客が見つからない場合の例外
 */
public class CustomerNotFoundException extends BusinessException {

    public CustomerNotFoundException(String email) {
        super("顧客が見つかりません。メールアドレス: " + email);
    }

    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
