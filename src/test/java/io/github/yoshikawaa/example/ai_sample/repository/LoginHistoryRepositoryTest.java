package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("LoginHistoryRepository のテスト")
class LoginHistoryRepositoryTest {

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        // テストで使用する顧客データを事前に挿入（外部キー制約のため）
        customerRepository.insert(createCustomer("test@example.com"));
        customerRepository.insert(createCustomer("other@example.com"));
        for (int i = 0; i < 5; i++) {
            customerRepository.insert(createCustomer("user" + i + "@example.com"));
        }
        customerRepository.insert(createCustomer("test1@example.com"));
        customerRepository.insert(createCustomer("test2@example.com"));
    }

    private Customer createCustomer(String email) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setPassword("password");
        customer.setName("Test User");
        customer.setRegistrationDate(LocalDate.now());
        customer.setBirthDate(LocalDate.of(1990, 1, 1));
        customer.setPhoneNumber("000-0000-0000");
        customer.setAddress("Test Address");
        customer.setRole(Customer.Role.USER);  // USER を使用
        return customer;
    }

    @Test
    @DisplayName("insert: ログイン履歴を保存できる")
    void testInsert() {
        LoginHistory history = new LoginHistory();
        history.setEmail("test@example.com");
        history.setLoginTime(LocalDateTime.now());
        history.setStatus(LoginHistory.Status.SUCCESS);
        history.setIpAddress("192.168.1.1");
        history.setUserAgent("Mozilla/5.0");
        history.setFailureReason(null);

        loginHistoryRepository.insert(history);

        assertThat(history.getId()).isNotNull();
    }

    @Test
    @DisplayName("findAllWithPagination: ページネーションで全件取得できる")
    void testFindAllWithPagination() {
        // テストデータを挿入
        for (int i = 0; i < 5; i++) {
            LoginHistory history = new LoginHistory();
            history.setEmail("user" + i + "@example.com");
            history.setLoginTime(LocalDateTime.now());
            history.setStatus(LoginHistory.Status.SUCCESS);
            history.setIpAddress("192.168.1." + i);
            history.setUserAgent("Mozilla/5.0");
            loginHistoryRepository.insert(history);
        }

        List<LoginHistory> histories = loginHistoryRepository.findAllWithPagination(3, 0, "login_time", "DESC");

        assertThat(histories).hasSize(3);
    }

    @Test
    @DisplayName("count: 全件数を取得できる")
    void testCount() {
        // テストデータを挿入
        for (int i = 0; i < 5; i++) {
            LoginHistory history = new LoginHistory();
            history.setEmail("user" + i + "@example.com");
            history.setLoginTime(LocalDateTime.now());
            history.setStatus(LoginHistory.Status.SUCCESS);
            history.setIpAddress("192.168.1." + i);
            history.setUserAgent("Mozilla/5.0");
            loginHistoryRepository.insert(history);
        }

        long count = loginHistoryRepository.count();

        assertThat(count).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("searchWithPagination: メールアドレスで検索できる")
    void testSearchWithPagination() {
        // テストデータを挿入
        LoginHistory history1 = new LoginHistory();
        history1.setEmail("test@example.com");
        history1.setLoginTime(LocalDateTime.now());
        history1.setStatus(LoginHistory.Status.SUCCESS);
        history1.setIpAddress("192.168.1.1");
        history1.setUserAgent("Mozilla/5.0");
        loginHistoryRepository.insert(history1);

        LoginHistory history2 = new LoginHistory();
        history2.setEmail("other@example.com");
        history2.setLoginTime(LocalDateTime.now());
        history2.setStatus(LoginHistory.Status.FAILURE);
        history2.setIpAddress("192.168.1.2");
        history2.setUserAgent("Mozilla/5.0");
        history2.setFailureReason("パスワード誤り");
        loginHistoryRepository.insert(history2);

        List<LoginHistory> histories = loginHistoryRepository.searchWithPagination(
            "test", null, null, null, 10, 0, "login_time", "DESC"
        );

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("searchWithPagination: ステータスで検索できる")
    void testSearchWithPaginationByStatus() {
        // テストデータを挿入
        LoginHistory history1 = new LoginHistory();
        history1.setEmail("test1@example.com");
        history1.setLoginTime(LocalDateTime.now());
        history1.setStatus(LoginHistory.Status.SUCCESS);
        history1.setIpAddress("192.168.1.1");
        history1.setUserAgent("Mozilla/5.0");
        loginHistoryRepository.insert(history1);

        LoginHistory history2 = new LoginHistory();
        history2.setEmail("test2@example.com");
        history2.setLoginTime(LocalDateTime.now());
        history2.setStatus(LoginHistory.Status.FAILURE);
        history2.setIpAddress("192.168.1.2");
        history2.setUserAgent("Mozilla/5.0");
        history2.setFailureReason("パスワード誤り");
        loginHistoryRepository.insert(history2);

        List<LoginHistory> histories = loginHistoryRepository.searchWithPagination(
            null, "FAILURE", null, null, 10, 0, "login_time", "DESC"
        );

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getStatus()).isEqualTo(LoginHistory.Status.FAILURE);
    }

    @Test
    @DisplayName("searchWithPagination: 日付範囲で検索できる")
    void testSearchWithPaginationByDateRange() {
        // テストデータを挿入
        LoginHistory history = new LoginHistory();
        history.setEmail("test@example.com");
        history.setLoginTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        history.setStatus(LoginHistory.Status.SUCCESS);
        history.setIpAddress("192.168.1.1");
        history.setUserAgent("Mozilla/5.0");
        loginHistoryRepository.insert(history);

        List<LoginHistory> histories = loginHistoryRepository.searchWithPagination(
            null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), 10, 0, "login_time", "DESC"
        );

        assertThat(histories).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("countBySearch: 検索条件での件数を取得できる")
    void testCountBySearch() {
        // テストデータを挿入
        LoginHistory history1 = new LoginHistory();
        history1.setEmail("test@example.com");
        history1.setLoginTime(LocalDateTime.now());
        history1.setStatus(LoginHistory.Status.SUCCESS);
        history1.setIpAddress("192.168.1.1");
        history1.setUserAgent("Mozilla/5.0");
        loginHistoryRepository.insert(history1);

        LoginHistory history2 = new LoginHistory();
        history2.setEmail("other@example.com");
        history2.setLoginTime(LocalDateTime.now());
        history2.setStatus(LoginHistory.Status.FAILURE);
        history2.setIpAddress("192.168.1.2");
        history2.setUserAgent("Mozilla/5.0");
        history2.setFailureReason("パスワード誤り");
        loginHistoryRepository.insert(history2);

        long count = loginHistoryRepository.countBySearch("test", null, null, null);

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

}
