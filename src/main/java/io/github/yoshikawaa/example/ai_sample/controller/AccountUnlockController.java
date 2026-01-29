package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.service.AccountLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
@RequestMapping("/account-unlock")
public class AccountUnlockController {
    private final AccountLockService accountLockService;


    @GetMapping("/request")
    public String showUnlockRequest() {
        return "account-unlock-request";
    }

    @PostMapping("/request")
    public String requestUnlock(@RequestParam String email, @RequestParam String name) {
        accountLockService.requestUnlock(email, name);
        return "redirect:/account-unlock/request-complete";
    }

    @GetMapping("/request-complete")
    public String showUnlockRequestComplete() {
        return "account-unlock-request-complete";
    }

    @GetMapping
    public String unlock(@RequestParam String token, Model model) {
        boolean success = accountLockService.unlockAccount(token);
        model.addAttribute("success", success);
        return "account-unlock-complete";
    }
}
