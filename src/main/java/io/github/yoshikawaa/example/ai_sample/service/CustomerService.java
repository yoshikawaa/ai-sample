package io.github.yoshikawaa.example.ai_sample.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public void registerCustomer(Customer customer) {
        // 未成年チェック
        if (isUnderage(customer.getBirthDate())) {
            throw new IllegalArgumentException("未成年の登録はできません。");
        }

        // パスワードをハッシュ化
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));

        // 顧客情報を登録
        customerRepository.save(customer);
    }

    public void changePassword(Customer customer, String newPassword) {
        // 新しいパスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(newPassword);

        // パスワードを更新
        customerRepository.updatePassword(customer.getEmail(), hashedPassword);

        // 認証情報を更新
        customer.setPassword(hashedPassword);
        CustomerUserDetails updatedUserDetails = new CustomerUserDetails(customer);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(updatedUserDetails, null, updatedUserDetails.getAuthorities())
        );
    }

    private boolean isUnderage(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();
        return age < 18; // 18歳未満を未成年とする
    }
}
