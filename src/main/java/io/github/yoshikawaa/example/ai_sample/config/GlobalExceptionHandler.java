package io.github.yoshikawaa.example.ai_sample.config;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 顧客が見つからない場合のハンドラー
     * 汎用エラー画面を表示（詳細表示や編集画面からの遷移を想定）
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCustomerNotFoundException(CustomerNotFoundException ex, Model model) {
        log.warn("Customer not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "404");
        return "error";
    }
}
