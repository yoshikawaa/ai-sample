package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationTypeCount;
import io.github.yoshikawaa.example.ai_sample.model.StatusCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("NotificationHistoryRepository のテスト")
class NotificationHistoryRepositoryTest {

    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        // テスト用顧客データを作成（外部キー制約対応）
        insertCustomer("test@example.com", "Test User");
        insertCustomer("user@example.com", "User");
        
        for (int i = 1; i <= 10; i++) {
            insertCustomer("user" + i + "@example.com", "User " + i);
        }
        
        insertCustomer("john@example.com", "John Doe");
        insertCustomer("jane@example.com", "Jane Doe");
    }

    private void insertCustomer(String email, String name) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setPassword("password");
        customer.setName(name);
        customer.setRegistrationDate(LocalDate.now());
        customer.setBirthDate(LocalDate.of(1990, 1, 1));
        customer.setPhoneNumber("000-0000-0000");
        customer.setAddress("Test Address");
        customer.setRole(Customer.Role.USER);
        customerRepository.insert(customer);
    }

    private void insertNotificationHistory(String recipientEmail, String notificationType, String subject,
                                            String status, LocalDateTime sentAt) {
        NotificationHistory notification = new NotificationHistory();
        notification.setRecipientEmail(recipientEmail);
        notification.setNotificationType(NotificationHistory.NotificationType.valueOf(notificationType));
        notification.setSubject(subject);
        notification.setBody("Test body");
        notification.setStatus(NotificationHistory.Status.valueOf(status));
        notification.setErrorMessage(null);
        notification.setSentAt(sentAt);
        notification.setCreatedAt(LocalDateTime.now());
        notificationHistoryRepository.insert(notification);
    }

    // ========================================
    // 全件取得系
    // ========================================

    @Nested
    @DisplayName("findAllWithPagination: 全件取得（ページネーション、ソート対応）")
    class FindAllWithPaginationTest {

        @Test
        @DisplayName("全件取得できる")
        void testFindAllWithPagination() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now().minusDays(1));
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.now());

            List<NotificationHistory> result = notificationHistoryRepository.findAllWithPagination(10, 0, null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("ページネーションが正しく動作する")
        void testFindAllWithPagination_Pagination() {
            for (int i = 1; i <= 5; i++) {
                insertNotificationHistory("user" + i + "@example.com", "PASSWORD_RESET", "Subject " + i, "SUCCESS", LocalDateTime.now().minusDays(i));
            }

            List<NotificationHistory> page1 = notificationHistoryRepository.findAllWithPagination(2, 0, null, null);
            List<NotificationHistory> page2 = notificationHistoryRepository.findAllWithPagination(2, 2, null, null);

            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
        }

        @Test
        @DisplayName("ソートが正しく動作する（sent_at DESC）")
        void testFindAllWithPagination_Sort() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.of(2023, 1, 1, 10, 0));
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.of(2023, 2, 1, 10, 0));

            List<NotificationHistory> result = notificationHistoryRepository.findAllWithPagination(10, 0, "sent_at", "DESC");

            assertThat(result.get(0).getRecipientEmail()).isEqualTo("user2@example.com");
            assertThat(result.get(1).getRecipientEmail()).isEqualTo("user1@example.com");
        }
    }

    @Nested
    @DisplayName("count: 全件数取得")
    class CountTest {

        @Test
        @DisplayName("全件数を取得できる")
        void testCount() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.now());

            long count = notificationHistoryRepository.count();

            assertThat(count).isEqualTo(2);
        }
    }

    // ========================================
    // 検索系
    // ========================================

    @Nested
    @DisplayName("searchWithPagination: 検索（ページネーション、ソート対応）")
    class SearchWithPaginationTest {

        @Test
        @DisplayName("受信者メールアドレスで検索できる")
        void testSearchWithPagination_ByRecipientEmail() {
            insertNotificationHistory("john@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("jane@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.now());

            List<NotificationHistory> result = notificationHistoryRepository.searchWithPagination(
                "john", null, null, null, null, 10, 0, null, null
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("通知種別で検索できる")
        void testSearchWithPagination_ByNotificationType() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.now());

            List<NotificationHistory> result = notificationHistoryRepository.searchWithPagination(
                null, NotificationHistory.NotificationType.PASSWORD_RESET, null, null, null, 10, 0, null, null
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationHistory.NotificationType.PASSWORD_RESET);
        }

        @Test
        @DisplayName("ステータスで検索できる")
        void testSearchWithPagination_ByStatus() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.now());

            List<NotificationHistory> result = notificationHistoryRepository.searchWithPagination(
                null, null, NotificationHistory.Status.SUCCESS, null, null, 10, 0, null, null
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(NotificationHistory.Status.SUCCESS);
        }

        @Test
        @DisplayName("期間で検索できる")
        void testSearchWithPagination_ByDateRange() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.of(2023, 1, 1, 10, 0));
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.of(2023, 2, 1, 10, 0));

            List<NotificationHistory> result = notificationHistoryRepository.searchWithPagination(
                null, null, null,
                LocalDateTime.of(2023, 1, 15, 0, 0),
                LocalDateTime.of(2023, 2, 15, 0, 0),
                10, 0, null, null
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientEmail()).isEqualTo("user2@example.com");
        }

        @Test
        @DisplayName("複数条件で検索できる")
        void testSearchWithPagination_MultipleConditions() {
            insertNotificationHistory("john@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.of(2023, 1, 1, 10, 0));
            insertNotificationHistory("john@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.of(2023, 2, 1, 10, 0));
            insertNotificationHistory("jane@example.com", "PASSWORD_RESET", "Subject 3", "SUCCESS", LocalDateTime.of(2023, 1, 15, 10, 0));

            List<NotificationHistory> result = notificationHistoryRepository.searchWithPagination(
                "john",
                NotificationHistory.NotificationType.PASSWORD_RESET,
                NotificationHistory.Status.SUCCESS,
                null, null, 10, 0, null, null
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientEmail()).isEqualTo("john@example.com");
            assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationHistory.NotificationType.PASSWORD_RESET);
        }
    }

    @Nested
    @DisplayName("countBySearch: 検索件数取得")
    class CountBySearchTest {

        @Test
        @DisplayName("検索条件に一致する件数を取得できる")
        void testCountBySearch() {
            insertNotificationHistory("john@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("john@example.com", "ACCOUNT_LOCK", "Subject 2", "FAILURE", LocalDateTime.now());
            insertNotificationHistory("jane@example.com", "PASSWORD_RESET", "Subject 3", "SUCCESS", LocalDateTime.now());

            long count = notificationHistoryRepository.countBySearch(
                "john", null, null, null, null
            );

            assertThat(count).isEqualTo(2);
        }
    }

    // ========================================
    // 単一取得
    // ========================================

    @Nested
    @DisplayName("findById: IDで取得")
    class FindByIdTest {

        @Test
        @DisplayName("IDで取得できる")
        void testFindById() {
            insertNotificationHistory("user@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());

            // 挿入したデータのIDを取得（IDは自動採番されるため）
            List<NotificationHistory> all = notificationHistoryRepository.findAllWithPagination(1, 0, null, null);
            Long id = all.get(0).getId();
            
            NotificationHistory result = notificationHistoryRepository.findById(id);

            assertThat(result).isNotNull();
            assertThat(result.getRecipientEmail()).isEqualTo("user@example.com");
        }
    }

    // ========================================
    // 登録
    // ========================================

    @Nested
    @DisplayName("insert: 通知履歴を登録")
    class InsertTest {

        @Test
        @DisplayName("通知履歴を登録できる")
        void testInsert() {
            NotificationHistory notification = new NotificationHistory();
            notification.setRecipientEmail("test@example.com");
            notification.setNotificationType(NotificationHistory.NotificationType.PASSWORD_RESET);
            notification.setSubject("Test Subject");
            notification.setBody("Test Body");
            notification.setStatus(NotificationHistory.Status.SUCCESS);
            notification.setErrorMessage(null);
            notification.setSentAt(LocalDateTime.now());
            notification.setCreatedAt(LocalDateTime.now());

            notificationHistoryRepository.insert(notification);

            // 検索して確認
            List<NotificationHistory> result = notificationHistoryRepository.searchWithPagination(
                "test", null, null, null, null, 10, 0, null, null
            );
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipientEmail()).isEqualTo("test@example.com");
        }
    }

    // ========================================
    // 統計
    // ========================================

    @Nested
    @DisplayName("countByNotificationType: 通知種別ごとの送信数を取得")
    class CountByNotificationTypeTest {

        @Test
        @DisplayName("通知種別ごとの送信数を取得できる")
        void testCountByNotificationType() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user2@example.com", "PASSWORD_RESET", "Subject 2", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user3@example.com", "ACCOUNT_LOCK", "Subject 3", "FAILURE", LocalDateTime.now());

            List<NotificationTypeCount> result = notificationHistoryRepository.countByNotificationType();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNotificationType()).isEqualTo("PASSWORD_RESET");
            assertThat(result.get(0).getCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countByStatus: ステータスごとの送信数を取得")
    class CountByStatusTest {

        @Test
        @DisplayName("ステータスごとの送信数を取得できる")
        void testCountByStatus() {
            insertNotificationHistory("user1@example.com", "PASSWORD_RESET", "Subject 1", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user2@example.com", "ACCOUNT_LOCK", "Subject 2", "SUCCESS", LocalDateTime.now());
            insertNotificationHistory("user3@example.com", "PASSWORD_RESET", "Subject 3", "FAILURE", LocalDateTime.now());

            List<StatusCount> result = notificationHistoryRepository.countByStatus();

            assertThat(result).hasSize(2);
            assertThat(result.stream().filter(c -> c.getStatus().equals("SUCCESS")).findFirst().get().getCount()).isEqualTo(2);
            assertThat(result.stream().filter(c -> c.getStatus().equals("FAILURE")).findFirst().get().getCount()).isEqualTo(1);
        }
    }
}
