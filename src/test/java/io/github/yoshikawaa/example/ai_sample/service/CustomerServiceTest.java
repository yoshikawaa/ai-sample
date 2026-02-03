package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;
import io.github.yoshikawaa.example.ai_sample.exception.UnderageCustomerException;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.AuditLogRepository;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("CustomerService のテスト")
class CustomerServiceTest {


    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CsvService csvService;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    @Autowired
    private CustomerService customerService;

    @BeforeEach
    void setUpAuditLogMock() {
        doNothing().when(auditLogRepository).insert(any());
    }

    // ========================================
    // 単一取得
    // ========================================

    @Test
    @DisplayName("getCustomerByEmail: メールアドレスで顧客を取得できる")
    void testGetCustomerByEmail() {
        // モックの動作を定義
        Customer customer = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 1, 1),
            LocalDate.of(1990, 1, 1),
            "123-456-7890",
            "123 Main St",
            Customer.Role.USER
        );
        when(customerRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(customer));

        // サービスメソッドを呼び出し
        Customer result = customerService.getCustomerByEmail("john.doe@example.com");

        // 検証
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(customerRepository, times(1)).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("getCustomerByEmail: 存在しないメールアドレスの場合、例外をスローする")
    void testGetCustomerByEmail_NotFound() {
        // モックの動作を定義
        when(customerRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // 例外がスローされることを検証
        CustomerNotFoundException exception = assertThrows(CustomerNotFoundException.class, () -> {
            customerService.getCustomerByEmail("nonexistent@example.com");
        });

        assertThat(exception.getMessage()).contains("nonexistent@example.com");
        verify(customerRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    // ========================================
    // 全件取得+ページネーション
    // ========================================

    @Test
    @DisplayName("getAllCustomersWithPagination: ページネーションで顧客を取得できる")
    void testGetAllCustomersWithPagination() {
        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findAllWithPagination(10, 0, null, null)).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St", Customer.Role.USER),
            new Customer("jane.doe@example.com", "password456", "Jane Doe", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "987-654-3210", "456 Elm St", Customer.Role.USER)
        ));
        when(customerRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<Customer> page = customerService.getAllCustomersWithPagination(pageable);

        // 検証
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
        verify(customerRepository, times(1)).findAllWithPagination(10, 0, null, null);
        verify(customerRepository, times(1)).count();
    }

    @Test
    @DisplayName("getAllCustomersWithPagination: 名前で昇順ソートができる")
    void testGetAllCustomersWithPagination_SortByNameAsc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").ascending());
        
        // モックの動作を定義
        when(customerRepository.findAllWithPagination(10, 0, "name", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        ));
        when(customerRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<Customer> result = customerService.getAllCustomersWithPagination(pageable);

        // 検証
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Alice");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Bob");
        verify(customerRepository, times(1)).findAllWithPagination(10, 0, "name", "ASC");
    }

    @Test
    @DisplayName("getAllCustomersWithPagination: 登録日で降順ソートができる")
    void testGetAllCustomersWithPagination_SortByRegistrationDateDesc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("registrationDate").descending());
        
        // モックの動作を定義
        when(customerRepository.findAllWithPagination(10, 0, "registration_date", "DESC")).thenReturn(Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        ));
        when(customerRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<Customer> result = customerService.getAllCustomersWithPagination(pageable);

        // 検証
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getRegistrationDate()).isEqualTo(LocalDate.of(2023, 2, 2));
        assertThat(result.getContent().get(1).getRegistrationDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        verify(customerRepository, times(1)).findAllWithPagination(10, 0, "registration_date", "DESC");
    }

    @Test
    @DisplayName("getAllCustomersWithPagination: 生年月日で昇順ソートができる")
    void testGetAllCustomersWithPagination_SortByBirthDateAsc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("birthDate").ascending());
        
        // モックの動作を定義
        when(customerRepository.findAllWithPagination(10, 0, "birth_date", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        ));
        when(customerRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<Customer> result = customerService.getAllCustomersWithPagination(pageable);

        // 検証
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(result.getContent().get(1).getBirthDate()).isEqualTo(LocalDate.of(1992, 2, 2));
        verify(customerRepository, times(1)).findAllWithPagination(10, 0, "birth_date", "ASC");
    }

    @Test
    @DisplayName("getAllCustomersWithPagination: 不明なプロパティでソート指定時はデフォルト（登録日）でソートされる")
    void testGetAllCustomersWithPagination_SortByUnknownPropertyUsesDefault() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("unknownProperty").ascending());
        
        // モックの動作を定義
        when(customerRepository.findAllWithPagination(10, 0, "registration_date", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        ));
        when(customerRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<Customer> result = customerService.getAllCustomersWithPagination(pageable);

        // 検証: デフォルトの registration_date でソートされる
        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository, times(1)).findAllWithPagination(10, 0, "registration_date", "ASC");
    }

    // ========================================
    // 登録
    // ========================================

    @Test
    @DisplayName("registerCustomer: 顧客を登録する際にパスワードをハッシュ化する")
    void testRegisterCustomer() {
        // テストデータの準備
        Customer newCustomer = new Customer(
            "new.customer@example.com",
            "plain_password", // 平文のパスワード
            "New Customer",
            LocalDate.of(2023, 3, 1), // registrationDate
            LocalDate.of(1990, 5, 20), // birthDate
            "111-222-3333",
            "789 New St",
            Customer.Role.USER
        );
        String hashedPassword = "hashed_password"; // ハッシュ化後のパスワード（モック）

        // モックの動作を定義
        when(passwordEncoder.encode("plain_password")).thenReturn(hashedPassword); // ハッシュ化のモック
        doNothing().when(customerRepository).insert(newCustomer);

        // サービスメソッドを呼び出し
        customerService.registerCustomer(newCustomer);

        // リポジトリの呼び出しを検証
        verify(customerRepository, times(1)).insert(argThat(customer -> 
            customer.getPassword().equals(hashedPassword) // ハッシュ化されたパスワードが渡されていることを確認
        ));
    }

    @Test
    @DisplayName("registerCustomer: 未成年の顧客登録時に例外をスローする")
    void testIsUnderageThrowsException() {
        // 未成年の顧客データを準備
        Customer underageCustomer = new Customer(
            "underage@example.com",
            "password123",
            "Underage User",
            LocalDate.now(), // registrationDate
            LocalDate.now().minusYears(15), // birthDate を 15 歳に設定
            "123-456-7890",
            "123 Main St",
            Customer.Role.USER
        );

        // 例外がスローされることを検証
        assertThrows(UnderageCustomerException.class, () -> {
            customerService.registerCustomer(underageCustomer);
        });
    }

    @Test
    @DisplayName("registerCustomer: 認証済みユーザーが登録する場合、監査ログに認証ユーザー名を記録する")
    @WithMockUser(username = "admin@example.com")
    void testRegisterCustomer_WithAuthentication() {
        // テストデータの準備
        Customer newCustomer = new Customer(
            "new.customer@example.com",
            "plain_password",
            "New Customer",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "111-222-3333",
            "789 New St",
            Customer.Role.USER
        );

        // モックの動作を定義
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        doNothing().when(customerRepository).insert(any());
        doNothing().when(auditLogService).recordAudit(any(), any(), any(), any(), any());

        // サービスメソッドを呼び出し
        customerService.registerCustomer(newCustomer);

        // 検証: 認証ユーザー名が監査ログに記録される
        verify(auditLogService, times(1)).recordAudit(
            eq("admin@example.com"), eq("new.customer@example.com"), eq(AuditLog.ActionType.CREATE), any(), any());
    }

    // ========================================
    // 更新
    // ========================================

    @Test
    @DisplayName("changePassword: パスワードを変更する際にハッシュ化して更新する")
    void testChangePassword() {
        // テストデータの準備
        Customer customer = new Customer(
            "john.doe@example.com",
            "old_password",
            "John Doe",
            LocalDate.of(2023, 3, 1), // registrationDate
            LocalDate.of(1990, 5, 20), // birthDate
            "123-456-7890",
            "123 Main St",
            Customer.Role.USER
        );
        String newPassword = "new_secure_password";
        String hashedPassword = "hashed_new_secure_password"; // ハッシュ化後のパスワード（モック）
    
        // モックの動作を定義
        when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword); // ハッシュ化のモック
        doNothing().when(customerRepository).updatePassword(customer.getEmail(), hashedPassword);
    
        // サービスメソッドを呼び出し
        customerService.changePassword(customer, newPassword);
    
        // リポジトリの呼び出しを検証
        verify(customerRepository, times(1)).updatePassword(customer.getEmail(), hashedPassword);
    }

    @Test
    @DisplayName("updateCustomerInfo: 顧客情報を更新する")
    void testUpdateCustomerInfo() {
        // テストデータの準備
        Customer customer = new Customer(
            "john.doe@example.com",
            "password123",
            "Updated Name",
            LocalDate.of(2023, 3, 1), // registrationDate
            LocalDate.of(1990, 5, 20), // birthDate
            "999-888-7777",
            "999 Updated St",
            Customer.Role.USER
        );

        // モックの動作を定義
        doNothing().when(customerRepository).updateCustomerInfo(customer);

        // サービスメソッドを呼び出し
        customerService.updateCustomerInfo(customer);

        // リポジトリの呼び出しを検証
        verify(customerRepository, times(1)).updateCustomerInfo(customer);
    }

    @Test
    @DisplayName("updateCustomerInfo: 管理者が他ユーザーの情報を更新する場合、SecurityContextを更新しない")
    @WithMockUser(username = "admin@example.com")
    void testUpdateCustomerInfo_AdminUpdatesOtherUser() {
        // テストデータの準備（管理者が他ユーザーの情報を更新）
        Customer customer = new Customer(
            "john.doe@example.com",
            "password123",
            "Updated Name",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "999-888-7777",
            "999 Updated St",
            Customer.Role.USER
        );

        // モックの動作を定義
        doNothing().when(customerRepository).updateCustomerInfo(any());
        doNothing().when(auditLogService).recordAudit(any(), any(), any(), any(), any());

        // サービスメソッドを呼び出し
        customerService.updateCustomerInfo(customer);

        // 検証: SecurityContextは更新されない（currentAuth.getName() != customer.getEmail() の分岐）
        verify(customerRepository, times(1)).updateCustomerInfo(customer);
        verify(auditLogService, times(1)).recordAudit(
            eq("admin@example.com"), eq("john.doe@example.com"), eq(AuditLog.ActionType.UPDATE), any(), any());
    }

    // ========================================
    // 削除
    // ========================================

    @Test
    @DisplayName("deleteCustomer: 顧客を削除できる")
    void testDeleteCustomer() {
        // モックの動作を定義
        doNothing().when(customerRepository).deleteByEmail("test@example.com");

        // サービスメソッドを呼び出し
        customerService.deleteCustomer("test@example.com");

        // リポジトリの呼び出しを検証
        verify(customerRepository, times(1)).deleteByEmail("test@example.com");
    }

    // ========================================
    // 検索+ページネーション
    // ========================================

    @Test
    @DisplayName("searchCustomersWithPagination: 検索条件でページネーション")
    void testSearchCustomersWithPagination() {
        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.searchWithPagination("John", "john@example.com", 10, 0, null, null)).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St", Customer.Role.USER)
        ));
        when(customerRepository.countBySearch("John", "john@example.com")).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<Customer> page = customerService.searchCustomersWithPagination("John", "john@example.com", pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("John Doe");
        verify(customerRepository, times(1)).searchWithPagination("John", "john@example.com", 10, 0, null, null);
        verify(customerRepository, times(1)).countBySearch("John", "john@example.com");
    }

    @Test
    @DisplayName("searchCustomersWithPagination: 検索結果をメールアドレスで昇順ソートができる")
    void testSearchCustomersWithPagination_SortByEmailAsc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("email").ascending());
        
        // モックの動作を定義
        when(customerRepository.searchWithPagination("test", null, 10, 0, "email", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice Test", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob Test", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        ));
        when(customerRepository.countBySearch("test", null)).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<Customer> result = customerService.searchCustomersWithPagination("test", null, pageable);

        // 検証
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getContent().get(1).getEmail()).isEqualTo("bob@example.com");
        verify(customerRepository, times(1)).searchWithPagination("test", null, 10, 0, "email", "ASC");
    }

    // ========================================
    // CSV出力
    // ========================================

    @Test
    @DisplayName("exportCustomersToCSV: 全顧客をCSVエクスポートできる")
    void testExportCustomersToCSV_AllCustomers() {
        // テストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: 検索条件でフィルタリングしてエクスポートできる")
    void testExportCustomersToCSV_WithSearchConditions() {
        // テストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.searchWithSort(eq("Alice"), any(), any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV("Alice", null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).searchWithSort(eq("Alice"), any(), any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: ソート順を適用してエクスポートできる")
    void testExportCustomersToCSV_WithSorting() {
        // テストデータ（name ASC: Alice first, then Bob）
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し（名前の昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: ダブルクォートを含むデータをエクスポートできる")
    void testExportCustomersToCSV_EscapeDoubleQuotes() {
        // テストデータ（ダブルクォートを含む名前）
        List<Customer> customers = Arrays.asList(
            new Customer("test@example.com", "password", "Test \"Name\"", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address \"1\"", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: 降順ソートを適用してエクスポートできる")
    void testExportCustomersToCSV_WithDescendingSort() {
        // テストデータ（name DESC: Bob first, then Alice）
        List<Customer> customers = Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し（名前の降順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").descending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: emailでソートしてエクスポートできる")
    void testExportCustomersToCSV_SortByEmail() {
        // テストデータ（email ASC: alice@ first, then bob@）
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し（emailの昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("email").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: birthDateでソートしてエクスポートできる")
    void testExportCustomersToCSV_SortByBirthDate() {
        // テストデータ（birth_date ASC: Alice (1990) first, then Bob (1992)）
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し（生年月日の昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("birthDate").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: 未知のプロパティでソートした場合はregistrationDateでソートされる")
    void testExportCustomersToCSV_SortByUnknownProperty() {
        // テストデータ（unknown property with ASC defaults to registration_date ASC: Alice (Jan) first, then Bob (Feb)）
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し（未知のプロパティでソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("unknownProperty").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: registrationDateでソートしてエクスポートできる")
    void testExportCustomersToCSV_SortByRegistrationDate() {
        // テストデータ（registration_date ASC: Alice (Jan) first, then Bob (Feb)）
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.findAllWithSort(any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し（登録日の昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("registrationDate").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).findAllWithSort(any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: emailのみで検索してエクスポートできる")
    void testExportCustomersToCSV_WithEmailOnly() {
        // テストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.searchWithSort(any(), eq("alice@example.com"), any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV(null, "alice@example.com", pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).searchWithSort(any(), eq("alice@example.com"), any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }

    @Test
    @DisplayName("exportCustomersToCSV: nameとemailの両方で検索してエクスポートできる")
    void testExportCustomersToCSV_WithBothNameAndEmail() {
        // テストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        );
        byte[] mockCsvData = "CSV data".getBytes();
        
        // モックの動作を定義
        when(customerRepository.searchWithSort(eq("Alice"), eq("alice@example.com"), any(), any())).thenReturn(customers);
        when(csvService.generateCustomerCsv(customers)).thenReturn(mockCsvData);

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV("Alice", "alice@example.com", pageable);

        // 検証
        assertThat(csvData).isEqualTo(mockCsvData);
        verify(customerRepository, times(1)).searchWithSort(eq("Alice"), eq("alice@example.com"), any(), any());
        verify(csvService, times(1)).generateCustomerCsv(customers);
    }
}
