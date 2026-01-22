package io.github.yoshikawaa.example.ai_sample.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;

/**
 * グローバル例外ハンドラー
 * 複数のコントローラで共通するビジネスロジック例外を処理します
 * コントローラ固有の例外は各コントローラで@ExceptionHandlerを実装します
 * HTTPステータスエラー（404、500等）はCustomErrorControllerで処理されます
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 顧客が見つからない場合のハンドラー
     * 汎用エラー画面を表示（詳細表示や編集画面からの遷移を想定）
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCustomerNotFoundException(CustomerNotFoundException ex, Model model) {
        logger.warn("Customer not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "404");
        return "error";
    }
}
