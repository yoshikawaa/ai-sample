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

    // ========================================
    // 全件取得系
    // ========================================

    @Test
    @DisplayName("findAllWithPagination: ページネーションで顧客を取得できる")
    void testFindAllWithPagination() {
        // 1ページ目（5件取得、offset=0）
        List<Customer> page1 = customerRepository.findAllWithPagination(5, 0, null, null);
        assertThat(page1).hasSize(5);

        // 2ページ目（5件取得、offset=5）
        List<Customer> page2 = customerRepository.findAllWithPagination(5, 5, null, null);
        assertThat(page2).hasSize(5);

        // 3ページ目（5件取得、offset=10）
        List<Customer> page3 = customerRepository.findAllWithPagination(5, 10, null, null);
        assertThat(page3).hasSize(5); // data.sqlには15件

        // 4ページ目（5件取得、offset=15）- データなし
        List<Customer> page4 = customerRepository.findAllWithPagination(5, 15, null, null);
        assertThat(page4).isEmpty();

        // 登録日の降順であることを確認
        assertThat(page1.get(0).getRegistrationDate()).isAfterOrEqualTo(page1.get(1).getRegistrationDate());
        
        // ページ間の順序を確認（page1の最後 >= page2の最初）
        assertThat(page1.get(4).getRegistrationDate()).isAfterOrEqualTo(page2.get(0).getRegistrationDate());
    }

    @Test
    @DisplayName("count: 全顧客数を取得できる")
    void testCount() {
        long count = customerRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(15);
    }

    // ========================================
    // 検索系
    // ========================================

    @Test
    @DisplayName("searchWithPagination: 検索条件でページネーション")
    void testSearchWithPagination() {
        // 名前で検索（1ページ目）
        List<Customer> results = customerRepository.searchWithPagination("Doe", null, 2, 0, null, null);
        assertThat(results).hasSizeLessThanOrEqualTo(2);
        assertThat(results).allMatch(c -> c.getName().toLowerCase().contains("doe"));
    }

    @Test
    @DisplayName("countBySearch: 検索結果の件数を取得できる")
    void testCountBySearch() {
        // 名前で検索
        long count = customerRepository.countBySearch("Doe", null);
        assertThat(count).isGreaterThanOrEqualTo(2); // John Doe, Jane Doe

        // 該当なし
        long noResults = customerRepository.countBySearch("NonExistent", null);
        assertThat(noResults).isZero();
    }

    // ========================================
    // 単一取得
    // ========================================

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
    @DisplayName("findByEmail: 存在しないメールアドレスの場合、空を返す")
    void testFindByEmail_存在しないメールアドレス() {
        // 存在しないメールアドレスで検索
        Optional<Customer> customer = customerRepository.findByEmail("non-existent@example.com");

        // 検証
        assertThat(customer).isNotPresent();
    }

    // ========================================
    // 登録
    // ========================================

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

    // ========================================
    // 更新
    // ========================================

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

    // ========================================
    // 削除
    // ========================================

    @Test
    @DisplayName("deleteByEmail: メールアドレスで顧客を削除できる")
    void testDeleteByEmail() {
        // テスト用の顧客を作成して保存
        Customer testCustomer = new Customer();
        testCustomer.setEmail("delete-test@example.com");
        testCustomer.setPassword("password");
        testCustomer.setName("Delete Test");
        testCustomer.setRegistrationDate(LocalDate.now());
        testCustomer.setBirthDate(LocalDate.of(1990, 1, 1));
        testCustomer.setPhoneNumber("999-999-9999");
        testCustomer.setAddress("999 Delete St");
        customerRepository.save(testCustomer);

        // 顧客が保存されたことを確認
        Optional<Customer> savedCustomer = customerRepository.findByEmail("delete-test@example.com");
        assertThat(savedCustomer).isPresent();

        // 顧客を削除
        customerRepository.deleteByEmail("delete-test@example.com");

        // 顧客が削除されたことを確認
        Optional<Customer> deletedCustomer = customerRepository.findByEmail("delete-test@example.com");
        assertThat(deletedCustomer).isNotPresent();
    }

    @Test
    @DisplayName("deleteByEmail: 存在しないメールアドレスでもエラーが発生しない")
    void testDeleteByEmail_存在しないメールアドレス() {
        // 存在しないメールアドレスで削除（エラーが発生しないことを確認）
        customerRepository.deleteByEmail("non-existent-delete@example.com");

        // 顧客が存在しないことを確認
        Optional<Customer> customer = customerRepository.findByEmail("non-existent-delete@example.com");
        assertThat(customer).isNotPresent();
    }
}
