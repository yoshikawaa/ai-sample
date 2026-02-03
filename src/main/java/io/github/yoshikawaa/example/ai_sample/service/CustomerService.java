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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;
import io.github.yoshikawaa.example.ai_sample.exception.UnderageCustomerException;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;
import io.github.yoshikawaa.example.ai_sample.util.RequestContextUtil;
import io.github.yoshikawaa.example.ai_sample.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    private final CsvService csvService;
    private final AuditLogService auditLogService;

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
        customer.setPassword(passwordEncoderProvider.getObject().encode(customer.getPassword()));

        // 顧客情報を登録
        customerRepository.insert(customer);
        
        // 監査ログを記録
        String performedBy = SecurityContextUtil.getAuthenticatedUsername(customer.getEmail());
        auditLogService.recordAudit(performedBy, customer.getEmail(), AuditLog.ActionType.CREATE, 
            "顧客登録: name=" + customer.getName(), RequestContextUtil.getClientIpAddress());
        
        log.info("顧客登録完了: email={}", customer.getEmail());
    }

    public void changePassword(Customer customer, String newPassword) {
        log.info("パスワード変更開始: email={}", customer.getEmail());
        
        // 新しいパスワードをハッシュ化
        String hashedPassword = passwordEncoderProvider.getObject().encode(newPassword);

        // パスワードを更新
        customerRepository.updatePassword(customer.getEmail(), hashedPassword);

        // 監査ログを記録
        String performedBy = SecurityContextUtil.getAuthenticatedUsername(customer.getEmail());
        auditLogService.recordAudit(performedBy, customer.getEmail(), AuditLog.ActionType.PASSWORD_RESET, 
            "パスワード変更", RequestContextUtil.getClientIpAddress());

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

        // 監査ログを記録
        String performedBy = SecurityContextUtil.getAuthenticatedUsername(customer.getEmail());
        auditLogService.recordAudit(performedBy, customer.getEmail(), AuditLog.ActionType.UPDATE, 
            "顧客情報更新: name=" + customer.getName(), RequestContextUtil.getClientIpAddress());

        // 認証情報を更新（ログインユーザー自身の情報を更新した場合のみ）
        if (performedBy.equals(customer.getEmail())) {
            CustomerUserDetails updatedUserDetails = new CustomerUserDetails(customer);
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(updatedUserDetails, null, updatedUserDetails.getAuthorities())
            );
            log.info("認証情報更新: email={}", customer.getEmail());
        }
        
        log.info("顧客情報更新完了: email={}", customer.getEmail());
    }

    public void deleteCustomer(String email) {
        log.info("顧客削除開始: email={}", email);
        
        // 監査ログを記録（削除前に記録）
        String performedBy = SecurityContextUtil.getAuthenticatedUsername(email);
        auditLogService.recordAudit(performedBy, email, AuditLog.ActionType.DELETE, 
            "顧客削除", RequestContextUtil.getClientIpAddress());
        
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
