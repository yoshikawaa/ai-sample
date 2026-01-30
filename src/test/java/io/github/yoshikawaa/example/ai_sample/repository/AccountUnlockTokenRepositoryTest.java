
package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.AccountUnlockToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import io.github.yoshikawaa.example.ai_sample.model.Customer;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AccountUnlockTokenRepository のテスト")
class AccountUnlockTokenRepositoryTest {

    @Autowired
    private AccountUnlockTokenRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("unlock_tokenで検索できる")
    void testFindByToken() {
        // 事前にcustomer登録
        Customer customer = new Customer("test@example.com", "dummy", "テストユーザー", java.time.LocalDate.now(), java.time.LocalDate.now(), "", "");
        customerRepository.insert(customer);
        // テストデータ登録
        AccountUnlockToken token = new AccountUnlockToken("test@example.com", "unlock-token", System.currentTimeMillis() + 10000);
        repository.insert(token);
        AccountUnlockToken found = repository.findByToken("unlock-token");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("tokenで削除できる")
    void testDeleteByToken() {
        // 事前にcustomer登録
        Customer customer = new Customer("test@example.com", "dummy", "テストユーザー", java.time.LocalDate.now(), java.time.LocalDate.now(), "", "");
        customerRepository.insert(customer);
        AccountUnlockToken token = new AccountUnlockToken("test@example.com", "unlock-token", System.currentTimeMillis() + 10000);
        repository.insert(token);
        repository.deleteByToken("unlock-token");
        AccountUnlockToken found = repository.findByToken("unlock-token");
        assertThat(found).isNull();
    }
}
