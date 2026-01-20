package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("CustomerService のテスト")
class CustomerServiceTest {

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerService customerService;

    @Test
    @DisplayName("getAllCustomers: すべての顧客を取得できる")
    void testFindAllCustomers() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St"),
            new Customer("jane.doe@example.com", "password456", "Jane Doe", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "987-654-3210", "456 Elm St")
        ));

        // サービスメソッドを呼び出し
        var customers = customerService.getAllCustomers();

        // 検証
        assertThat(customers).hasSize(2);
        assertThat(customers.get(0).getName()).isEqualTo("John Doe");
        assertThat(customers.get(1).getName()).isEqualTo("Jane Doe");
    }

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
            "789 New St"
        );
        String hashedPassword = "hashed_password"; // ハッシュ化後のパスワード（モック）

        // モックの動作を定義
        when(passwordEncoder.encode("plain_password")).thenReturn(hashedPassword); // ハッシュ化のモック
        doNothing().when(customerRepository).save(newCustomer);

        // サービスメソッドを呼び出し
        customerService.registerCustomer(newCustomer);

        // リポジトリの呼び出しを検証
        verify(customerRepository, times(1)).save(argThat(customer -> 
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
            "123 Main St"
        );

        // 例外がスローされることを検証
        assertThrows(IllegalArgumentException.class, () -> {
            customerService.registerCustomer(underageCustomer);
        });
    }

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
            "123 Main St"
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
            "999 Updated St"
        );

        // モックの動作を定義
        doNothing().when(customerRepository).updateCustomerInfo(customer);

        // サービスメソッドを呼び出し
        customerService.updateCustomerInfo(customer);

        // リポジトリの呼び出しを検証
        verify(customerRepository, times(1)).updateCustomerInfo(customer);
    }

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

    @Test
    @DisplayName("searchCustomers: 顧客を検索できる")
    void testSearchCustomers() {
        // モックの動作を定義
        when(customerRepository.search("John", "john@example.com")).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St")
        ));

        // サービスメソッドを呼び出し
        var customers = customerService.searchCustomers("John", "john@example.com");

        // 検証
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getName()).isEqualTo("John Doe");
        verify(customerRepository, times(1)).search("John", "john@example.com");
    }

    @Test
    @DisplayName("getAllCustomersWithPagination: ページネーションで顧客を取得できる")
    void testGetAllCustomersWithPagination() {
        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findAllWithPagination(10, 0, null, null)).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St"),
            new Customer("jane.doe@example.com", "password456", "Jane Doe", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "987-654-3210", "456 Elm St")
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
    @DisplayName("searchCustomersWithPagination: 検索条件でページネーション")
    void testSearchCustomersWithPagination() {
        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.searchWithPagination("John", "john@example.com", 10, 0, null, null)).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St")
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
            "123 Main St"
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
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.getCustomerByEmail("nonexistent@example.com");
        });

        assertThat(exception.getMessage()).isEqualTo("顧客が見つかりません。");
        verify(customerRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("getAllCustomersWithPagination: 名前で昇順ソートができる")
    void testGetAllCustomersWithPagination_SortByNameAsc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").ascending());
        
        // モックの動作を定義
        when(customerRepository.findAllWithPagination(10, 0, "name", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1"),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2")
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
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2"),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
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
    @DisplayName("searchCustomersWithPagination: 検索結果をメールアドレスで昇順ソートができる")
    void testSearchCustomersWithPagination_SortByEmailAsc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("email").ascending());
        
        // モックの動作を定義
        when(customerRepository.searchWithPagination("test", null, 10, 0, "email", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice Test", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1"),
            new Customer("bob@example.com", "password", "Bob Test", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2")
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

    @Test
    @DisplayName("getAllCustomersWithPagination: 生年月日で昇順ソートができる")
    void testGetAllCustomersWithPagination_SortByBirthDateAsc() {
        // テストデータの準備
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("birthDate").ascending());
        
        // モックの動作を定義
        when(customerRepository.findAllWithPagination(10, 0, "birth_date", "ASC")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1"),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2")
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
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));
        when(customerRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<Customer> result = customerService.getAllCustomersWithPagination(pageable);

        // 検証: デフォルトの registration_date でソートされる
        assertThat(result.getContent()).hasSize(1);
        verify(customerRepository, times(1)).findAllWithPagination(10, 0, "registration_date", "ASC");
    }

    @Test
    @DisplayName("exportCustomersToCSV: 全顧客をCSVエクスポートできる")
    void exportCustomersToCSV_AllCustomers() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1"),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2")
        ));

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // UTF-8 BOMを確認
        assertThat(csv).startsWith("\uFEFF");
        
        // ヘッダーを確認
        assertThat(csv).contains("Email,Name,Registration Date,Birth Date,Phone Number,Address");
        
        // データを確認
        assertThat(csv).contains("alice@example.com");
        assertThat(csv).contains("Alice");
        assertThat(csv).contains("bob@example.com");
        assertThat(csv).contains("Bob");
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: 検索条件でフィルタリングしてエクスポートできる")
    void exportCustomersToCSV_WithSearchConditions() {
        // モックの動作を定義
        when(customerRepository.search("Alice", null)).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV("Alice", null, pageable);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        assertThat(csv).contains("alice@example.com");
        assertThat(csv).contains("Alice");
        assertThat(csv).doesNotContain("bob@example.com");
        
        verify(customerRepository, times(1)).search("Alice", null);
    }

    @Test
    @DisplayName("exportCustomersToCSV: ソート順を適用してエクスポートできる")
    void exportCustomersToCSV_WithSorting() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2"),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し（名前の昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: CSV内のデータが名前順にソートされていることを確認
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        int aliceIndex = csv.indexOf("Alice");
        int bobIndex = csv.indexOf("Bob");
        
        assertThat(aliceIndex).isLessThan(bobIndex);
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: ダブルクォートをエスケープできる")
    void exportCustomersToCSV_EscapeDoubleQuotes() {
        // モックの動作を定義（ダブルクォートを含む名前）
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("test@example.com", "password", "Test \"Name\"", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address \"1\"")
        ));

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: ダブルクォートがエスケープされていることを確認
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("Test \"\"Name\"\"");
        assertThat(csv).contains("Address \"\"1\"\"");
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: 降順ソートを適用してエクスポートできる")
    void exportCustomersToCSV_WithDescendingSort() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1"),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2")
        ));

        // サービスメソッドを呼び出し（名前の降順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").descending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: CSV内のデータが名前降順でソートされていることを確認
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        int aliceIndex = csv.indexOf("Alice");
        int bobIndex = csv.indexOf("Bob");
        
        assertThat(bobIndex).isLessThan(aliceIndex);
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: emailでソートしてエクスポートできる")
    void exportCustomersToCSV_SortByEmail() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2"),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し（emailの昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("email").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: CSV内のデータがemail順にソートされていることを確認
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        int aliceIndex = csv.indexOf("alice@example.com");
        int bobIndex = csv.indexOf("bob@example.com");
        
        assertThat(aliceIndex).isLessThan(bobIndex);
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: birthDateでソートしてエクスポートできる")
    void exportCustomersToCSV_SortByBirthDate() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2"),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し（生年月日の昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("birthDate").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: CSV内のデータが生年月日順にソートされていることを確認
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        int aliceIndex = csv.indexOf("Alice");
        int bobIndex = csv.indexOf("Bob");
        
        assertThat(aliceIndex).isLessThan(bobIndex);
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: 未知のプロパティでソートした場合はregistrationDateでソートされる")
    void exportCustomersToCSV_SortByUnknownProperty() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2"),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し（未知のプロパティでソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("unknownProperty").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: CSV内のデータが登録日順にソートされていることを確認（デフォルト動作）
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        int aliceIndex = csv.indexOf("Alice");
        int bobIndex = csv.indexOf("Bob");
        
        assertThat(aliceIndex).isLessThan(bobIndex);
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: registrationDateでソートしてエクスポートできる")
    void exportCustomersToCSV_SortByRegistrationDate() {
        // モックの動作を定義
        when(customerRepository.findAll()).thenReturn(Arrays.asList(
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2"),
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し（登録日の昇順でソート）
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("registrationDate").ascending());
        byte[] csvData = customerService.exportCustomersToCSV(null, null, pageable);

        // 検証: CSV内のデータが登録日順にソートされていることを確認
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        int aliceIndex = csv.indexOf("Alice");
        int bobIndex = csv.indexOf("Bob");
        
        assertThat(aliceIndex).isLessThan(bobIndex);
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: CSV生成時にエラーが発生した場合、RuntimeExceptionがスローされる")
    void exportCustomersToCSV_WithInvalidData() {
        // リポジトリがnullを返すことでNullPointerExceptionを誘発
        when(customerRepository.findAll()).thenReturn(null);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // CSV生成時に例外がスローされることを確認
        assertThrows(RuntimeException.class, () -> {
            customerService.exportCustomersToCSV(null, null, pageable);
        });
        
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("exportCustomersToCSV: emailのみで検索してエクスポートできる")
    void exportCustomersToCSV_WithEmailOnly() {
        // モックの動作を定義
        when(customerRepository.search(null, "alice@example.com")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV(null, "alice@example.com", pageable);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        assertThat(csv).contains("alice@example.com");
        assertThat(csv).contains("Alice");
        
        verify(customerRepository, times(1)).search(null, "alice@example.com");
    }

    @Test
    @DisplayName("exportCustomersToCSV: nameとemailの両方で検索してエクスポートできる")
    void exportCustomersToCSV_WithBothNameAndEmail() {
        // モックの動作を定義
        when(customerRepository.search("Alice", "alice@example.com")).thenReturn(Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1")
        ));

        // サービスメソッドを呼び出し
        Pageable pageable = PageRequest.of(0, 10);
        byte[] csvData = customerService.exportCustomersToCSV("Alice", "alice@example.com", pageable);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        assertThat(csv).contains("alice@example.com");
        assertThat(csv).contains("Alice");
        
        verify(customerRepository, times(1)).search("Alice", "alice@example.com");
    }
}
