package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.exception.UnderageCustomerException;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerForm;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/register")
public class CustomerRegistrationController {

    private final CustomerService customerService;

    // 共通の @ModelAttribute メソッド
    @ModelAttribute("customerForm")
    public CustomerForm customerForm() {
        return new CustomerForm();
    }

    // 入力画面の表示
    @GetMapping("/input")
    public String showInputForm() {
        return "customer-input";
    }

    @PostMapping("/confirm")
    public String showConfirmForm(@Validated CustomerForm customerForm,
                                   BindingResult bindingResult) {
        // バリデーションエラーがある場合、入力画面に戻る
        if (bindingResult.hasErrors()) {
            return "customer-input";
        }
    
        return "customer-confirm";
    }
    
    @PostMapping("/input")
    public String handleBackToInput(CustomerForm customerForm) {
        return "customer-input";
    }
    
    // 登録処理
    @PostMapping("/register")
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
    
        return "redirect:/register/complete";
    }

    // 完了画面の表示
    @GetMapping("/complete")
    public String showCompletePage() {
        return "customer-complete";
    }

    /**
     * 未成年の顧客を登録しようとした場合のハンドラー
     * 顧客登録エラー画面を表示し、入力画面に戻れるようにする
     */
    @ExceptionHandler(UnderageCustomerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUnderageCustomerException(UnderageCustomerException ex, Model model) {
        log.warn("Underage customer registration attempt: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "400");
        return "customer-registration-error";
    }
}
