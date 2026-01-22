package io.github.yoshikawaa.example.ai_sample.exception;

/**
 * 未成年顧客の登録を試みた場合の例外
 */
public class UnderageCustomerException extends BusinessException {

    public UnderageCustomerException() {
        super("未成年の登録はできません。");
    }

    public UnderageCustomerException(String message) {
        super(message);
    }
}
