package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 実際のデータベースを使用
@ActiveProfiles("test") // テスト用プロファイルを使用
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testFindAll() {
        // データベースに登録されているすべての顧客を取得
        List<Customer> customers = customerRepository.findAll();

        // 検証
        assertThat(customers).isNotEmpty();
        assertThat(customers.size()).isGreaterThanOrEqualTo(3); // `data.sql` に基づく
    }

    @Test
    void testFindByEmail() {
        // データベースから特定の顧客を取得
        Optional<Customer> customer = customerRepository.findByEmail("john.doe@example.com");

        // 検証
        assertThat(customer).isPresent();
        assertThat(customer.get().getName()).isEqualTo("John Doe");
    }

    @Test
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
    void testUpdatePassword() {
        // パスワードを更新
        customerRepository.updatePassword("john.doe@example.com", "updated_password");

        // 更新された顧客を取得して検証
        Optional<Customer> updatedCustomer = customerRepository.findByEmail("john.doe@example.com");
        assertThat(updatedCustomer).isPresent();
        assertThat(updatedCustomer.get().getPassword()).isEqualTo("updated_password");
    }
}
