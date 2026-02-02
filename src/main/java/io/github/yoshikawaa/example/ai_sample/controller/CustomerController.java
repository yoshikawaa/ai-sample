package io.github.yoshikawaa.example.ai_sample.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerSearchForm;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/customers")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    @ModelAttribute("customerSearchForm")
    public CustomerSearchForm customerSearchForm() {
        return new CustomerSearchForm();
    }

    @GetMapping
    public String showCustomers(@PageableDefault(size = 10, sort = "registrationDate", direction = Direction.DESC) Pageable pageable,
                                Model model) {
        Page<Customer> customerPage = customerService.getAllCustomersWithPagination(pageable);
        model.addAttribute("customerPage", customerPage);
        return "customer-list";
    }

    @GetMapping("/search")
    public String searchCustomers(CustomerSearchForm customerSearchForm,
                                   @PageableDefault(size = 10, sort = "registrationDate", direction = Direction.DESC) Pageable pageable,
                                   Model model) {
        Page<Customer> customerPage = customerService.searchCustomersWithPagination(
            customerSearchForm.getName(), customerSearchForm.getEmail(), pageable);
        model.addAttribute("customerPage", customerPage);
        return "customer-list";
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

    @GetMapping("/{email}")
    public String showCustomerDetail(@PathVariable String email, Model model) {
        Customer customer = customerService.getCustomerByEmail(email);
        model.addAttribute("customer", customer);
        return "customer-detail";
    }
}
