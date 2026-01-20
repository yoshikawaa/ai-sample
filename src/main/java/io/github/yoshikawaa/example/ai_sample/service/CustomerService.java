package io.github.yoshikawaa.example.ai_sample.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。"));
    }

    public Page<Customer> getAllCustomersWithPagination(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<Customer> customers = customerRepository.findAllWithPagination(pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = customerRepository.count();
        return new PageImpl<>(customers, pageable, total);
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

    public void updateCustomerInfo(Customer customer) {
        // 顧客情報を更新
        customerRepository.updateCustomerInfo(customer);

        // 認証情報を更新
        CustomerUserDetails updatedUserDetails = new CustomerUserDetails(customer);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(updatedUserDetails, null, updatedUserDetails.getAuthorities())
        );
    }

    public void deleteCustomer(String email) {
        // 顧客を削除
        customerRepository.deleteByEmail(email);

        // 認証情報をクリア
        SecurityContextHolder.clearContext();
    }

    public List<Customer> searchCustomers(String name, String email) {
        return customerRepository.search(name, email);
    }

    public Page<Customer> searchCustomersWithPagination(String name, String email, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<Customer> customers = customerRepository.searchWithPagination(name, email, pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = customerRepository.countBySearch(name, email);
        return new PageImpl<>(customers, pageable, total);
    }

    private boolean isUnderage(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();
        return age < 18; // 18歳未満を未成年とする
    }

    private String[] extractSortInfo(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
            
            // プロパティ名をDBカラム名に変換
            String column = switch (property) {
                case "email" -> "email";
                case "name" -> "name";
                case "registrationDate" -> "registration_date";
                case "birthDate" -> "birth_date";
                default -> "registration_date";
            };
            
            return new String[]{column, direction};
        }
        return new String[]{null, null};
    }
}

