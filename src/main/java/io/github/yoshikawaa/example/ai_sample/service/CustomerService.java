package io.github.yoshikawaa.example.ai_sample.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CsvService csvService;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder, CsvService csvService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.csvService = csvService;
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

    public Page<Customer> searchCustomersWithPagination(String name, String email, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<Customer> customers = customerRepository.searchWithPagination(name, email, pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = customerRepository.countBySearch(name, email);
        return new PageImpl<>(customers, pageable, total);
    }

    public byte[] exportCustomersToCSV(String name, String email, Pageable pageable) {
        // 検索・ソート条件に基づいて顧客を取得（全件）
        String[] sortInfo = extractSortInfo(pageable);
        List<Customer> customers;
        
        if (StringUtils.hasText(name) || StringUtils.hasText(email)) {
            // 検索条件がある場合
            customers = customerRepository.searchWithSort(name, email, sortInfo[0], sortInfo[1]);
        } else {
            // 検索条件がない場合
            customers = customerRepository.findAllWithSort(sortInfo[0], sortInfo[1]);
        }
        
        return csvService.generateCustomerCsv(customers);
    }

    private boolean isUnderage(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();
        return age < 18; // 18歳未満を未成年とする
    }

    private String[] extractSortInfo(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
            
            // プロパティ名をDBカラム名に変換
            String column = switch (property) {
                case "email" -> "email";
                case "name" -> "name";
                case "birthDate" -> "birth_date";
                default -> "registration_date";
            };
            
            return new String[]{column, direction};
        }
        return new String[]{null, null};
    }
}
