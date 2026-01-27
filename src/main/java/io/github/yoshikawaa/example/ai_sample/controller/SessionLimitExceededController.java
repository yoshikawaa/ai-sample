package io.github.yoshikawaa.example.ai_sample.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/session-limit-exceeded")
public class SessionLimitExceededController {

    @GetMapping
    public String showSessionLimitExceeded(Model model) {
        log.warn("最大セッション数超過によるログイン拒否");
        model.addAttribute("errorMessage", "同時ログインの上限に達したため、これ以上ログインできません。\n他の端末でログアウトしてから再度お試しください。");
        model.addAttribute("errorCode", "SESSION_LIMIT_EXCEEDED");
        return "session-limit-exceeded";
    }
}
