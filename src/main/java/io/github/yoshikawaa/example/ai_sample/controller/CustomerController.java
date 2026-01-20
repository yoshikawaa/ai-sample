package io.github.yoshikawaa.example.ai_sample.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerSearchForm;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @ModelAttribute("customerSearchForm")
    public CustomerSearchForm customerSearchForm() {
        return new CustomerSearchForm();
    }

    @GetMapping
    public String showCustomers(@PageableDefault(size = 10) Pageable pageable,
                                Model model) {
        Page<Customer> customerPage = customerService.getAllCustomersWithPagination(pageable);
        model.addAttribute("customerPage", customerPage);
        return "customer-list";
    }

    @GetMapping("/search")
    public String searchCustomers(CustomerSearchForm customerSearchForm,
                                   @PageableDefault(size = 10) Pageable pageable,
                                   Model model) {
        Page<Customer> customerPage = customerService.searchCustomersWithPagination(
            customerSearchForm.getName(), customerSearchForm.getEmail(), pageable);
        model.addAttribute("customerPage", customerPage);
        return "customer-list";
    }
}
