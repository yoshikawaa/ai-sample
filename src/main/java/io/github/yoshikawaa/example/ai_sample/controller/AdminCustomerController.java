package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.exception.UnderageCustomerException;
import io.github.yoshikawaa.example.ai_sample.model.AdminCustomerEditForm;
import io.github.yoshikawaa.example.ai_sample.model.AdminCustomerRegistrationForm;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerSearchForm;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;

    // ========================================
    // 顧客一覧・検索
    // ========================================

    @ModelAttribute("customerSearchForm")
    public CustomerSearchForm customerSearchForm() {
        return new CustomerSearchForm();
    }

    @GetMapping
    public String showCustomers(@PageableDefault(size = 10, sort = "registrationDate", direction = Direction.DESC) Pageable pageable,
                                Model model) {
        Page<Customer> customerPage = customerService.getAllCustomersWithPagination(pageable);
        model.addAttribute("customerPage", customerPage);
        return "admin-customer-list";
    }

    @GetMapping("/search")
    public String searchCustomers(CustomerSearchForm customerSearchForm,
                                   @PageableDefault(size = 10, sort = "registrationDate", direction = Direction.DESC) Pageable pageable,
                                   Model model) {
        Page<Customer> customerPage = customerService.searchCustomersWithPagination(
            customerSearchForm.getName(), customerSearchForm.getEmail(), pageable);
        model.addAttribute("customerPage", customerPage);
        return "admin-customer-list";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCustomersToCSV(
            CustomerSearchForm customerSearchForm,
            @PageableDefault(size = 10, sort = "registrationDate", direction = Direction.DESC) Pageable pageable) {
        
        // CSV生成
        byte[] csvData = customerService.exportCustomersToCSV(
            customerSearchForm.getName(), 
            customerSearchForm.getEmail(), 
            pageable);
        
        // ファイル名生成（タイムスタンプ付き）
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "customers_" + timestamp + ".csv";
        
        // レスポンス設定
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvData);
    }

    // ========================================
    // 顧客詳細
    // ========================================

    @GetMapping("/{email}")
    public String showCustomerDetail(@PathVariable String email, Model model) {
        Customer customer = customerService.getCustomerByEmail(email);
        boolean isLocked = loginAttemptService.isLocked(email);
        model.addAttribute("customer", customer);
        model.addAttribute("isLocked", isLocked);
        return "admin-customer-detail";
    }

    // ========================================
    // 顧客新規登録（管理者による代理登録）
    // ========================================

    @ModelAttribute("adminCustomerRegistrationForm")
    public AdminCustomerRegistrationForm adminCustomerRegistrationForm() {
        return new AdminCustomerRegistrationForm();
    }

    @GetMapping("/registration-input")
    public String showRegistrationInputForm() {
        return "admin-customer-registration-input";
    }

    @PostMapping("/registration-confirm")
    public String showRegistrationConfirmForm(@Validated AdminCustomerRegistrationForm form,
                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin-customer-registration-input";
        }
        return "admin-customer-registration-confirm";
    }

    @PostMapping("/registration-input")
    public String handleBackToRegistrationInput(AdminCustomerRegistrationForm form) {
        return "admin-customer-registration-input";
    }

    @PostMapping("/registration")
    public String registerCustomer(@Validated AdminCustomerRegistrationForm form,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "admin-customer-registration-input";
        }

        Customer customer = new Customer();
        customer.setEmail(form.getEmail());
        customer.setPassword(form.getPassword());
        customer.setName(form.getName());
        customer.setBirthDate(form.getBirthDate());
        customer.setPhoneNumber(form.getPhoneNumber());
        customer.setAddress(form.getAddress());
        customer.setRegistrationDate(LocalDate.now());
        customer.setRole(form.getRole());

        customerService.registerCustomer(customer);

        return "redirect:/admin/customers/registration-complete";
    }

    @GetMapping("/registration-complete")
    public String showRegistrationCompletePage() {
        return "admin-customer-registration-complete";
    }

    // ========================================
    // 顧客編集機能
    // ========================================

    @ModelAttribute("adminCustomerEditForm")
    public AdminCustomerEditForm adminCustomerEditForm() {
        return new AdminCustomerEditForm();
    }

    @GetMapping("/{email}/edit-input")
    public String showEditInputForm(@PathVariable String email, Model model) {
        Customer customer = customerService.getCustomerByEmail(email);
        
        AdminCustomerEditForm form = new AdminCustomerEditForm();
        form.setName(customer.getName());
        form.setBirthDate(customer.getBirthDate());
        form.setPhoneNumber(customer.getPhoneNumber());
        form.setAddress(customer.getAddress());
        form.setRole(customer.getRole());
        
        model.addAttribute("adminCustomerEditForm", form);
        model.addAttribute("email", email);
        return "admin-customer-edit-input";
    }

    @PostMapping("/{email}/edit-confirm")
    public String showEditConfirmForm(@PathVariable String email,
                                       @Validated AdminCustomerEditForm form,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", email);
            return "admin-customer-edit-input";
        }
        model.addAttribute("email", email);
        return "admin-customer-edit-confirm";
    }

    @PostMapping("/{email}/edit-input")
    public String handleBackToEditInput(@PathVariable String email,
                                          AdminCustomerEditForm form,
                                          Model model) {
        model.addAttribute("email", email);
        return "admin-customer-edit-input";
    }

    @PostMapping("/{email}/edit")
    public String updateCustomer(@PathVariable String email,
                                  @Validated AdminCustomerEditForm form,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", email);
            return "admin-customer-edit-input";
        }

        Customer customer = customerService.getCustomerByEmail(email);
        customer.setName(form.getName());
        customer.setBirthDate(form.getBirthDate());
        customer.setPhoneNumber(form.getPhoneNumber());
        customer.setAddress(form.getAddress());
        customer.setRole(form.getRole());

        customerService.updateCustomerInfo(customer);

        return "admin-customer-edit-complete";
    }

    // ========================================
    // 顧客削除機能
    // ========================================

    @GetMapping("/{email}/delete-confirm")
    public String showDeleteConfirmForm(@PathVariable String email, Model model) {
        Customer customer = customerService.getCustomerByEmail(email);
        model.addAttribute("customer", customer);
        return "admin-customer-delete-confirm";
    }

    @PostMapping("/{email}/delete")
    public String deleteCustomer(@PathVariable String email) {
        customerService.deleteCustomer(email);
        return "admin-customer-delete-complete";
    }

    // ========================================
    // アカウントロック/アンロック操作
    // ========================================

    @PostMapping("/{email}/lock")
    public String lockAccount(@PathVariable String email, Model model) {
        loginAttemptService.lockAccountByAdmin(email);
        model.addAttribute("email", email);
        return "admin-account-lock-complete";
    }

    @PostMapping("/{email}/unlock")
    public String unlockAccount(@PathVariable String email, Model model) {
        loginAttemptService.unlockAccountByAdmin(email);
        model.addAttribute("email", email);
        return "admin-account-unlock-complete";
    }

    // ========================================
    // パスワードリセットリンク発行
    // ========================================

    @PostMapping("/{email}/password-reset")
    public String sendPasswordResetLink(@PathVariable String email, Model model) {
        passwordResetService.sendResetLink(email);
        model.addAttribute("email", email);
        return "admin-password-reset-complete";
    }

    // ========================================
    // 例外ハンドラ
    // ========================================

    /**
     * 未成年の顧客を登録しようとした場合のハンドラー（管理者用）
     * 管理者用顧客登録エラー画面を表示し、入力画面に戻れるようにする
     */
    @ExceptionHandler(UnderageCustomerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUnderageCustomerException(UnderageCustomerException ex, Model model) {
        log.warn("Admin - Underage customer registration attempt: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "400");
        return "admin-customer-registration-error";
    }
}
