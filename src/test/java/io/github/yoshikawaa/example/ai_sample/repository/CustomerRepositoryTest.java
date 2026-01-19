package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 実際のデータベースを使用
@DisplayName("CustomerRepository のテスト")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("findAll: すべての顧客を取得できる")
    void testFindAll() {
        // データベースに登録されているすべての顧客を取得
        List<Customer> customers = customerRepository.findAll();

        // 検証
        assertThat(customers).isNotEmpty();
        assertThat(customers.size()).isGreaterThanOrEqualTo(3); // `data.sql` に基づく
    }

    @Test
    @DisplayName("findByEmail: 特定のメールアドレスで顧客を取得できる")
    void testFindByEmail() {
        // データベースから特定の顧客を取得
        Optional<Customer> customer = customerRepository.findByEmail("john.doe@example.com");

        // 検証
        assertThat(customer).isPresent();
        assertThat(customer.get().getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("save: 新しい顧客を保存できる")
    void testSave() {
        // 新しい顧客を作成
        Customer newCustomer = new Customer();
        newCustomer.setEmail("new.customer@example.com");
        newCustomer.setPassword("new_password");
        newCustomer.setName("New Customer");
        newCustomer.setRegistrationDate(LocalDate.now());
        newCustomer.setBirthDate(LocalDate.of(1995, 5, 20));
        newCustomer.setPhoneNumber("111-222-3333");
        newCustomer.setAddress("789 New St");

        // 顧客を保存
        customerRepository.save(newCustomer);

        // 保存された顧客を取得して検証
        Optional<Customer> savedCustomer = customerRepository.findByEmail("new.customer@example.com");
        assertThat(savedCustomer).isPresent();
        assertThat(savedCustomer.get().getName()).isEqualTo("New Customer");
    }

    @Test
    @DisplayName("updatePassword: パスワードを更新できる")
    void testUpdatePassword() {
        // パスワードを更新
        customerRepository.updatePassword("john.doe@example.com", "updated_password");

        // 更新された顧客を取得して検証
        Optional<Customer> updatedCustomer = customerRepository.findByEmail("john.doe@example.com");
        assertThat(updatedCustomer).isPresent();
        assertThat(updatedCustomer.get().getPassword()).isEqualTo("updated_password");
    }

    @Test
    @DisplayName("updatePassword: 存在しないメールアドレスでもエラーが発生しない")
    void testUpdatePassword_存在しないメールアドレス() {
        // 存在しないメールアドレスでパスワードを更新（エラーが発生しないことを確認）
        customerRepository.updatePassword("non-existent@example.com", "new_password");

        // 顧客が存在しないことを確認
        Optional<Customer> customer = customerRepository.findByEmail("non-existent@example.com");
        assertThat(customer).isNotPresent();
    }

    @Test
    @DisplayName("updatePassword: 複数回更新できる")
    void testUpdatePassword_複数回更新() {
        // 最初の更新
        customerRepository.updatePassword("john.doe@example.com", "password1");
        Optional<Customer> customer1 = customerRepository.findByEmail("john.doe@example.com");
        assertThat(customer1).isPresent();
        assertThat(customer1.get().getPassword()).isEqualTo("password1");

        // 2回目の更新
        customerRepository.updatePassword("john.doe@example.com", "password2");
        Optional<Customer> customer2 = customerRepository.findByEmail("john.doe@example.com");
        assertThat(customer2).isPresent();
        assertThat(customer2.get().getPassword()).isEqualTo("password2");

        // 3回目の更新
        customerRepository.updatePassword("john.doe@example.com", "password3");
        Optional<Customer> customer3 = customerRepository.findByEmail("john.doe@example.com");
        assertThat(customer3).isPresent();
        assertThat(customer3.get().getPassword()).isEqualTo("password3");
    }

    @Test
    @DisplayName("findByEmail: 存在しないメールアドレスの場合、空を返す")
    void testFindByEmail_存在しないメールアドレス() {
        // 存在しないメールアドレスで検索
        Optional<Customer> customer = customerRepository.findByEmail("non-existent@example.com");

        // 検証
        assertThat(customer).isNotPresent();
    }

    @Test
    @DisplayName("updateCustomerInfo: 顧客情報を更新できる")
    void testUpdateCustomerInfo() {
        // 更新前の顧客を取得
        Optional<Customer> beforeCustomer = customerRepository.findByEmail("john.doe@example.com");
        assertThat(beforeCustomer).isPresent();

        // 顧客情報を更新
        Customer updatedCustomer = beforeCustomer.get();
        updatedCustomer.setName("John Updated");
        updatedCustomer.setBirthDate(LocalDate.of(1991, 2, 2));
        updatedCustomer.setPhoneNumber("999-888-7777");
        updatedCustomer.setAddress("999 Updated St");
        customerRepository.updateCustomerInfo(updatedCustomer);

        // 更新後の顧客を取得して検証
        Optional<Customer> afterCustomer = customerRepository.findByEmail("john.doe@example.com");
        assertThat(afterCustomer).isPresent();
        assertThat(afterCustomer.get().getName()).isEqualTo("John Updated");
        assertThat(afterCustomer.get().getBirthDate()).isEqualTo(LocalDate.of(1991, 2, 2));
        assertThat(afterCustomer.get().getPhoneNumber()).isEqualTo("999-888-7777");
        assertThat(afterCustomer.get().getAddress()).isEqualTo("999 Updated St");
        // パスワードとメールアドレスは更新されないことを確認
        assertThat(afterCustomer.get().getPassword()).isEqualTo(beforeCustomer.get().getPassword());
        assertThat(afterCustomer.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("updateCustomerInfo: 存在しないメールアドレスでもエラーが発生しない")
    void testUpdateCustomerInfo_存在しないメールアドレス() {
        // 存在しない顧客の情報を更新（エラーが発生しないことを確認）
        Customer nonExistentCustomer = new Customer();
        nonExistentCustomer.setEmail("non-existent@example.com");
        nonExistentCustomer.setName("Non Existent");
        nonExistentCustomer.setBirthDate(LocalDate.of(1990, 1, 1));
        nonExistentCustomer.setPhoneNumber("000-000-0000");
        nonExistentCustomer.setAddress("000 Non St");
        customerRepository.updateCustomerInfo(nonExistentCustomer);

        // 顧客が存在しないことを確認
        Optional<Customer> customer = customerRepository.findByEmail("non-existent@example.com");
        assertThat(customer).isNotPresent();
    }
}
