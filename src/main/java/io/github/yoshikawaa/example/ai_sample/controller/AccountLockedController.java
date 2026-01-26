package io.github.yoshikawaa.example.ai_sample.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;

@Controller
public class AccountLockedController {

    private final LoginAttemptService loginAttemptService;

    public AccountLockedController(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/account-locked")
    public String showAccountLockedPage(@RequestParam String email, Model model) {
        String lockedUntil = loginAttemptService.getLockedUntilFormatted(email);
        model.addAttribute("lockedUntil", lockedUntil);
        return "account-locked";
    }
}
