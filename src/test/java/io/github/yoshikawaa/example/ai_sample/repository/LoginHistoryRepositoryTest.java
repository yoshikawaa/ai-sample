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

    // ========================================
    // 特定顧客のログ取得
    // ========================================

    @Test
    @DisplayName("findByEmail: 特定顧客のログイン履歴を取得できる")
    void testFindByEmail() {
        // テストデータを挿入
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(3));
        insertLoginHistory("test@example.com", LoginHistory.Status.FAILURE, LocalDateTime.now().minusDays(2));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));
        insertLoginHistory("other@example.com", LoginHistory.Status.SUCCESS, LocalDateTime.now());

        // 特定顧客のログを取得
        List<LoginHistory> histories = loginHistoryRepository.findByEmail("test@example.com", null, null, 100);

        // 検証
        assertThat(histories).hasSize(3);
        assertThat(histories.get(0).getLoginTime()).isAfter(histories.get(1).getLoginTime());  // 新しい順
        assertThat(histories).allMatch(h -> h.getEmail().equals("test@example.com"));
    }

    @Test
    @DisplayName("findByEmail: 開始日で絞り込みができる")
    void testFindByEmail_WithStartDate() {
        LocalDateTime now = LocalDateTime.now();
        // テストデータを挿入
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now.minusDays(5));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now.minusDays(2));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now);

        // 3日前以降のログを取得
        LocalDateTime startDate = now.minusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<LoginHistory> histories = loginHistoryRepository.findByEmail("test@example.com", startDate, null, 100);

        // 検証: 2件のみ取得される
        assertThat(histories).hasSize(2);
    }

    @Test
    @DisplayName("findByEmail: 終了日で絞り込みができる")
    void testFindByEmail_WithEndDate() {
        LocalDateTime now = LocalDateTime.now();
        // テストデータを挿入
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now.minusDays(5));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now.minusDays(2));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now);

        // 3日前までのログを取得
        LocalDateTime endDate = now.minusDays(3).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<LoginHistory> histories = loginHistoryRepository.findByEmail("test@example.com", null, endDate, 100);

        // 検証: 1件のみ取得される
        assertThat(histories).hasSize(1);
    }

    @Test
    @DisplayName("findByEmail: 期間指定で絞り込みができる")
    void testFindByEmail_WithDateRange() {
        LocalDateTime now = LocalDateTime.now();
        // テストデータを挿入
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now.minusDays(10));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now.minusDays(5));
        insertLoginHistory("test@example.com", LoginHistory.Status.FAILURE, now.minusDays(3));
        insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, now);

        // 6日前から2日前までのログを取得
        LocalDateTime startDate = now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endDate = now.minusDays(2).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<LoginHistory> histories = loginHistoryRepository.findByEmail("test@example.com", startDate, endDate, 100);

        // 検証: 2件取得される
        assertThat(histories).hasSize(2);
    }

    @Test
    @DisplayName("findByEmail: limit数で取得件数を制限できる")
    void testFindByEmail_WithLimit() {
        // テストデータを10件挿入
        for (int i = 0; i < 10; i++) {
            insertLoginHistory("test@example.com", LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(i));
        }

        // limit=5で取得
        List<LoginHistory> histories = loginHistoryRepository.findByEmail("test@example.com", null, null, 5);

        // 検証: 5件のみ取得される
        assertThat(histories).hasSize(5);
    }

    @Test
    @DisplayName("findByEmail: 該当データがない場合は空リストを返す")
    void testFindByEmail_NoData() {
        // テストデータを挿入（別の顧客）
        insertLoginHistory("other@example.com", LoginHistory.Status.SUCCESS, LocalDateTime.now());

        // 存在しない顧客のログを取得
        List<LoginHistory> histories = loginHistoryRepository.findByEmail("nonexistent@example.com", null, null, 100);

        // 検証: 空リストが返される
        assertThat(histories).isEmpty();
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private void insertLoginHistory(String email, LoginHistory.Status status, LocalDateTime loginTime) {
        LoginHistory history = new LoginHistory();
        history.setEmail(email);
        history.setLoginTime(loginTime);
        history.setStatus(status);
        history.setIpAddress("192.168.1.1");
        history.setUserAgent("Mozilla/5.0");
        if (status == LoginHistory.Status.FAILURE) {
            history.setFailureReason("テスト失敗理由");
        }
        loginHistoryRepository.insert(history);
    }
}
