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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;
import io.github.yoshikawaa.example.ai_sample.exception.UnderageCustomerException;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CsvService csvService;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder, CsvService csvService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.csvService = csvService;
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
            .orElseThrow(() -> new CustomerNotFoundException(email));
    }

    @Transactional(readOnly = true)
    public Page<Customer> getAllCustomersWithPagination(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<Customer> customers = customerRepository.findAllWithPagination(pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = customerRepository.count();
        return new PageImpl<>(customers, pageable, total);
    }

    public void registerCustomer(Customer customer) {
        log.info("顧客登録開始: email={}", customer.getEmail());
        
        // 未成年チェック
        if (isUnderage(customer.getBirthDate())) {
            throw new UnderageCustomerException();
        }

        // パスワードをハッシュ化
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));

        // 顧客情報を登録
        customerRepository.save(customer);
        
        log.info("顧客登録完了: email={}", customer.getEmail());
    }

    public void changePassword(Customer customer, String newPassword) {
        log.info("パスワード変更開始: email={}", customer.getEmail());
        
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
        
        log.info("パスワード変更完了: email={}", customer.getEmail());
    }

    public void updateCustomerInfo(Customer customer) {
        log.info("顧客情報更新開始: email={}, name={}", customer.getEmail(), customer.getName());
        
        // 顧客情報を更新
        customerRepository.updateCustomerInfo(customer);

        // 認証情報を更新
        CustomerUserDetails updatedUserDetails = new CustomerUserDetails(customer);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(updatedUserDetails, null, updatedUserDetails.getAuthorities())
        );
        
        log.info("顧客情報更新完了: email={}", customer.getEmail());
    }

    public void deleteCustomer(String email) {
        log.info("顧客削除開始: email={}", email);
        
        // 顧客を削除
        customerRepository.deleteByEmail(email);

        // 認証情報をクリア
        SecurityContextHolder.clearContext();
        
        log.info("顧客削除完了: email={}", email);
    }

    @Transactional(readOnly = true)
    public Page<Customer> searchCustomersWithPagination(String name, String email, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<Customer> customers = customerRepository.searchWithPagination(name, email, pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = customerRepository.countBySearch(name, email);
        return new PageImpl<>(customers, pageable, total);
    }

    @Transactional(readOnly = true)
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
        
        log.info("CSVエクスポート実行: 件数={}, 検索条件(name={}, email={})", customers.size(), name, email);
        
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
