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
        when(customerRepository.findAllWithPagination(10, 0)).thenReturn(Arrays.asList(
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
        verify(customerRepository, times(1)).findAllWithPagination(10, 0);
        verify(customerRepository, times(1)).count();
    }

    @Test
    @DisplayName("searchCustomersWithPagination: 検索条件でページネーション")
    void testSearchCustomersWithPagination() {
        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.searchWithPagination("John", "john@example.com", 10, 0)).thenReturn(Arrays.asList(
            new Customer("john.doe@example.com", "password123", "John Doe", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-456-7890", "123 Main St")
        ));
        when(customerRepository.countBySearch("John", "john@example.com")).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<Customer> page = customerService.searchCustomersWithPagination("John", "john@example.com", pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("John Doe");
        verify(customerRepository, times(1)).searchWithPagination("John", "john@example.com", 10, 0);
        verify(customerRepository, times(1)).countBySearch("John", "john@example.com");
    }
}
