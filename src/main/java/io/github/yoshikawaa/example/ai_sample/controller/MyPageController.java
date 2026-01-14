package io.github.yoshikawaa.example.ai_sample.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.yoshikawaa.example.ai_sample.model.ChangePasswordForm;
import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;

@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final CustomerService customerService;

    public MyPageController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String showMyPage() {
        // 画面で #authentication を使用して Customer を取得するため、特別な処理は不要
        return "mypage";
    }

    @GetMapping("/change-password")
    public String showChangePasswordPage(ChangePasswordForm changePasswordForm) {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                 @Validated ChangePasswordForm changePasswordForm,
                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // バリデーションエラーがある場合、エラーメッセージを表示
            return "change-password";
        }
    
        // パスワード変更処理
        customerService.changePassword(userDetails.getCustomer(), changePasswordForm.getNewPassword());
        return "redirect:/mypage/change-password-complete";
    }

    @GetMapping("/change-password-complete")
    public String showChangePasswordCompletePage() {
        return "change-password-complete";
    }
}
