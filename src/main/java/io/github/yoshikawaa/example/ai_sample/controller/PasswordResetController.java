package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.exception.InvalidTokenException;
import io.github.yoshikawaa.example.ai_sample.model.PasswordResetForm;
import io.github.yoshikawaa.example.ai_sample.service.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@Controller
@RequestMapping("/password-reset") // クラスレベルで共通のパスを設定
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/request")
    public String showResetRequestForm() {
        return "password-reset-request"; // パスワードリセットリクエスト画面
    }

    @PostMapping("/request")
    public String handleResetRequest(@RequestParam(required = true) String email) {
        passwordResetService.sendResetLink(email);
        return "redirect:/password-reset/request-complete";
    }

    @GetMapping("/confirm")
    public String showResetForm(@RequestParam(required = false) String token, PasswordResetForm passwordResetForm, Model model) {
        passwordResetService.validateResetToken(token);
        model.addAttribute("token", token);
        return "password-reset-form";
    }

    @PostMapping("/reset")
    public String resetPassword(@Validated PasswordResetForm passwordResetForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "password-reset-form";
        }

        passwordResetService.updatePassword(passwordResetForm.getToken(), passwordResetForm.getNewPassword());
        return "redirect:/password-reset/complete";
    }

    @GetMapping("/complete")
    public String showResetCompletePage() {
        return "password-reset-complete"; // パスワードリセット完了画面
    }

    @GetMapping("/request-complete")
    public String showResetRequestCompletePage() {
        return "password-reset-request-complete";
    }

    /**
     * 無効なパスワードリセットトークンの場合のハンドラー
     * パスワードリセットエラー画面を表示し、リクエスト画面に戻れるようにする
     */
    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidTokenException(InvalidTokenException ex, Model model) {
        log.warn("Invalid password reset token: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "400");
        return "password-reset-error";
    }
}