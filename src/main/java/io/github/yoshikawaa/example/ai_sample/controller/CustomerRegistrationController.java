package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerForm;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class CustomerRegistrationController {

    private final CustomerService customerService;

    public CustomerRegistrationController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // 共通の @ModelAttribute メソッド
    @ModelAttribute("customerForm")
    public CustomerForm customerForm() {
        return new CustomerForm();
    }

    // 入力画面の表示
    @GetMapping("/customers/input")
    public String showInputForm() {
        return "customer-input";
    }

    @PostMapping("/customers/confirm")
    public String showConfirmForm(@Validated CustomerForm customerForm,
                                   BindingResult bindingResult) {
        // バリデーションエラーがある場合、入力画面に戻る
        if (bindingResult.hasErrors()) {
            return "customer-input";
        }
    
        return "customer-confirm";
    }
    
    @PostMapping("/customers/input")
    public String handleBackToInput(CustomerForm customerForm) {
        return "customer-input";
    }
    
    // 登録処理
    @PostMapping("/customers/register")
    public String registerCustomer(@Validated CustomerForm customerForm,
                                   BindingResult bindingResult) {
        // バリデーションエラーがある場合、入力画面に戻る
        if (bindingResult.hasErrors()) {
            return "customer-input";
        }
    
        // CustomerForm を Customer に変換
        Customer customer = new Customer();
        customer.setEmail(customerForm.getEmail());
        customer.setPassword(customerForm.getPassword());
        customer.setName(customerForm.getName());
        customer.setBirthDate(customerForm.getBirthDate());
        customer.setPhoneNumber(customerForm.getPhoneNumber());
        customer.setAddress(customerForm.getAddress());
        customer.setRegistrationDate(LocalDate.now());
    
        // 顧客情報を登録
        customerService.registerCustomer(customer);
    
        return "redirect:/customers/complete";
    }

    // 完了画面の表示
    @GetMapping("/customers/complete")
    public String showCompletePage() {
        return "customer-complete";
    }

    // ビジネスエラーのハンドリング
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBusinessError(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "business-error";
    }
}
