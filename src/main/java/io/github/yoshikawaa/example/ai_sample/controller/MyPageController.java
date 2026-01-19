package io.github.yoshikawaa.example.ai_sample.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.yoshikawaa.example.ai_sample.model.ChangePasswordForm;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerEditForm;
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

    @GetMapping("/edit")
    public String showEditPage(@AuthenticationPrincipal CustomerUserDetails userDetails, Model model) {
        Customer customer = userDetails.getCustomer();
        CustomerEditForm editForm = new CustomerEditForm();
        editForm.setName(customer.getName());
        editForm.setBirthDate(customer.getBirthDate());
        editForm.setPhoneNumber(customer.getPhoneNumber());
        editForm.setAddress(customer.getAddress());
        model.addAttribute("customerEditForm", editForm);
        return "customer-edit";
    }

    @PostMapping("/edit-confirm")
    public String showEditConfirmPage(@Validated CustomerEditForm customerEditForm,
                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "customer-edit";
        }
        return "customer-edit-confirm";
    }

    @PostMapping("/edit")
    public String handleBackToEdit(CustomerEditForm customerEditForm) {
        return "customer-edit";
    }

    @PostMapping("/update")
    public String updateCustomer(@AuthenticationPrincipal CustomerUserDetails userDetails,
                                  @Validated CustomerEditForm customerEditForm,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "customer-edit";
        }

        Customer customer = userDetails.getCustomer();
        customer.setName(customerEditForm.getName());
        customer.setBirthDate(customerEditForm.getBirthDate());
        customer.setPhoneNumber(customerEditForm.getPhoneNumber());
        customer.setAddress(customerEditForm.getAddress());

        customerService.updateCustomerInfo(customer);
        return "redirect:/mypage/edit-complete";
    }

    @GetMapping("/edit-complete")
    public String showEditCompletePage() {
        return "customer-edit-complete";
    }

    @GetMapping("/delete")
    public String showDeletePage() {
        return "customer-delete";
    }

    @PostMapping("/delete-confirm")
    public String showDeleteConfirmPage() {
        return "customer-delete-confirm";
    }

    @PostMapping("/delete")
    public String handleBackToDelete() {
        return "customer-delete";
    }

    @PostMapping("/delete-execute")
    public String deleteCustomer(@AuthenticationPrincipal CustomerUserDetails userDetails) {
        customerService.deleteCustomer(userDetails.getCustomer().getEmail());
        return "redirect:/mypage/delete-complete";
    }

    @GetMapping("/delete-complete")
    public String showDeleteCompletePage() {
        return "customer-delete-complete";
    }
}
