package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.PasswordResetForm;
import io.github.yoshikawaa.example.ai_sample.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public String handleResetRequest(@RequestParam String email, Model model) {
        try {
            passwordResetService.sendResetLink(email);
            model.addAttribute("message", "パスワードリセットリンクを送信しました。");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "password-reset-request";
    }

    @GetMapping("/confirm")
    public String showResetForm(@RequestParam(required = false) String token, PasswordResetForm passwordResetForm, Model model) {
        boolean isValid = passwordResetService.validateResetToken(token);
        if (isValid) {
            model.addAttribute("token", token);
            return "password-reset-form"; // Thymeleaf テンプレート名
        } else {
            model.addAttribute("errorMessage", "無効または期限切れのトークンです。");
            return "password-reset-error";
        }
    }

    @PostMapping("/reset")
    public String resetPassword(@Validated PasswordResetForm passwordResetForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "password-reset-form";
        }

        boolean isValid = passwordResetService.validateResetToken(passwordResetForm.getToken());
        if (isValid) {
            try {
                passwordResetService.updatePassword(passwordResetForm.getToken(), passwordResetForm.getNewPassword());
                return "redirect:/password-reset/complete";
            } catch (Exception e) {
                model.addAttribute("errorMessage", "パスワードリセット中にエラーが発生しました。");
                return "password-reset-error";
            }
        } else {
            model.addAttribute("errorMessage", "無効または期限切れのトークンです。");
            return "password-reset-error";
        }
    }

    @GetMapping("/complete")
    public String showResetCompletePage() {
        return "password-reset-complete"; // パスワードリセット完了画面
    }
}