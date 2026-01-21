package io.github.yoshikawaa.example.ai_sample.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerCsvDto;
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
        
        return generateCSV(customers);
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

    private byte[] generateCSV(List<Customer> customers) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            
            // UTF-8 BOMを追加（Excelでの文字化け防止）
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            // ヘッダー行を明示的に書き込み（@CsvBindByNameのカラム名を@CsvBindByPositionの順序で出力）
            osw.write("Email,Name,Registration Date,Birth Date,Phone Number,Address\n");
            osw.flush();
            
            // CustomerエンティティをCustomerCsvDtoに変換
            List<CustomerCsvDto> csvDtos = customers.stream()
                .map(CustomerCsvDto::fromEntity)
                .collect(Collectors.toList());
            
            // OpenCSVを使用してデータ行を生成（@CsvBindByPositionで順序制御）
            ColumnPositionMappingStrategy<CustomerCsvDto> strategy = 
                new ColumnPositionMappingStrategy<>();
            strategy.setType(CustomerCsvDto.class);
            
            StatefulBeanToCsv<CustomerCsvDto> beanToCsv = new StatefulBeanToCsvBuilder<CustomerCsvDto>(osw)
                .withMappingStrategy(strategy)
                .withApplyQuotesToAll(false)
                .build();
            
            beanToCsv.write(csvDtos);
            osw.flush();
            
            return baos.toByteArray();
        } catch (Exception e) {
            // NOTE: このcatchブロックは防御的プログラミングのために存在します。
            // ByteArrayOutputStreamとOpenCSVの通常動作では例外は発生しませんが、
            // 予期しないランタイムエラー（OutOfMemoryError等）からの保護として残しています。
            // テストでのカバレッジは困難ですが、本番環境での安全性のために必要です。
            throw new RuntimeException("CSV生成中にエラーが発生しました", e);
        }
    }
}
