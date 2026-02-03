package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
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
@DisplayName("AuditLogRepository のテスト")
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        // チE��ト用の顧客を挿入�E�外部キー制紁E��策！E
        Customer customer1 = createCustomer("test-admin@example.com");
        Customer customer2 = createCustomer("test-user@example.com");
        customerRepository.insert(customer1);
        customerRepository.insert(customer2);
    }

    private Customer createCustomer(String email) {
        return new Customer(
            email,
            "password",
            "Test User",
            LocalDate.now(),
            LocalDate.of(1990, 1, 1),
            "123-456-7890",
            "Test Address",
            Customer.Role.USER
        );
    }

    // ========================================
    // 登録
    // ========================================

    @Test
    @DisplayName("insert: 監査ログを挿入できる")
    void testInsert() {
        // チE��トデータ
        AuditLog auditLog = new AuditLog();
        auditLog.setPerformedBy("test-admin@example.com");
        auditLog.setTargetEmail("test-user@example.com");
        auditLog.setActionType(AuditLog.ActionType.CREATE);
        auditLog.setActionDetail("顧客登録");
        auditLog.setActionTime(LocalDateTime.now());
        auditLog.setIpAddress("192.168.1.1");

        // 挿入
        auditLogRepository.insert(auditLog);

        // 検証: IDが�E動採番されめE
        assertThat(auditLog.getId()).isNotNull();
        assertThat(auditLog.getId()).isGreaterThan(0);
    }

    // ========================================
    // 全件取得系
    // ========================================

    @Test
    @DisplayName("findAllWithPagination: ペ�Eジネ�Eションで全件取得できる")
    void testFindAllWithPagination() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now().minusDays(2));
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now().minusDays(1));
        insertAuditLog("test-admin@example.com", "test-user@example.com", "DELETE", "顧客削除", LocalDateTime.now());

        // 全件取得！Eimit=10, offset=0�E�E
        List<AuditLog> logs = auditLogRepository.findAllWithPagination(10, 0, null, null);

        // 検証
        assertThat(logs).hasSize(3);
        // チE��ォルトソーチE action_time DESC
        assertThat(logs.get(0).getActionType()).isEqualTo(AuditLog.ActionType.DELETE);
        assertThat(logs.get(1).getActionType()).isEqualTo(AuditLog.ActionType.UPDATE);
        assertThat(logs.get(2).getActionType()).isEqualTo(AuditLog.ActionType.CREATE);
    }

    @Test
    @DisplayName("findAllWithPagination: ソート指定！Ection_time ASC�E�で取得できる")
    void testFindAllWithPagination_SortByActionTimeAsc() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now().minusDays(2));
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now().minusDays(1));
        insertAuditLog("test-admin@example.com", "test-user@example.com", "DELETE", "顧客削除", LocalDateTime.now());

        // ソート指宁E action_time ASC
        List<AuditLog> logs = auditLogRepository.findAllWithPagination(10, 0, "action_time", "ASC");

        // 検証
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).getActionType()).isEqualTo(AuditLog.ActionType.CREATE);
        assertThat(logs.get(1).getActionType()).isEqualTo(AuditLog.ActionType.UPDATE);
        assertThat(logs.get(2).getActionType()).isEqualTo(AuditLog.ActionType.DELETE);
    }

    @Test
    @DisplayName("findAllWithPagination: ペ�EジサイズとオフセチE��を指定できる")
    void testFindAllWithPagination_WithOffset() {
        // チE��トデータめE件挿入
        String[] actionTypes = {"CREATE", "UPDATE", "DELETE", "PASSWORD_RESET", "ACCOUNT_LOCK"};
        for (int i = 0; i < 5; i++) {
            insertAuditLog("test-admin@example.com", "test-user@example.com", actionTypes[i], "詳細" + (i + 1), LocalDateTime.now().minusDays(4 - i));
        }

        // 2ペ�Eジ目を取得！Eimit=2, offset=2�E�E
        List<AuditLog> logs = auditLogRepository.findAllWithPagination(2, 2, "action_time", "DESC");

        // 検証: 3番目と4番目のレコードが取得される
        assertThat(logs).hasSize(2);
    }

    @Test
    @DisplayName("count: 全件数を取得できる")
    void testCount() {
        // チE��トデータめE件挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "DELETE", "顧客削除", LocalDateTime.now());

        // 件数取征E
        long count = auditLogRepository.count();

        // 検証
        assertThat(count).isEqualTo(3);
    }

    // ========================================
    // 検索系
    // ========================================

    @Test
    @DisplayName("searchWithPagination: performedByで検索できる")
    void testSearchWithPagination_ByPerformedBy() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-user@example.com", "test-admin@example.com", "UPDATE", "顧客更新", LocalDateTime.now());

        // 検索: performedBy="test-admin@example.com"
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            "test-admin@example.com", null, null, null, null, 10, 0, null, null
        );

        // 検証
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getPerformedBy()).isEqualTo("test-admin@example.com");
    }

    @Test
    @DisplayName("searchWithPagination: targetEmailで検索できる")
    void testSearchWithPagination_ByTargetEmail() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-admin@example.com", "UPDATE", "顧客更新", LocalDateTime.now());

        // 検索: targetEmail="test-user@example.com"
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            null, "test-user@example.com", null, null, null, 10, 0, null, null
        );

        // 検証
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getTargetEmail()).isEqualTo("test-user@example.com");
    }

    @Test
    @DisplayName("searchWithPagination: actionTypeで検索できる")
    void testSearchWithPagination_ByActionType() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "DELETE", "顧客削除", LocalDateTime.now());

        // 検索: actionType="CREATE"
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            null, null, AuditLog.ActionType.CREATE, null, null, 10, 0, null, null
        );

        // 検証
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getActionType()).isEqualTo(AuditLog.ActionType.CREATE);
    }

    @Test
    @DisplayName("searchWithPagination: 日付篁E��で検索できる")
    void testSearchWithPagination_ByDateRange() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.of(2024, 1, 1, 10, 0));
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.of(2024, 1, 15, 10, 0));
        insertAuditLog("test-admin@example.com", "test-user@example.com", "DELETE", "顧客削除", LocalDateTime.of(2024, 2, 1, 10, 0));

        // 検索: fromDate=2024-01-01, toDate=2024-01-31
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            null, null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), 10, 0, null, null
        );

        // 検証: 1月�Eログのみ取得される
        assertThat(logs).hasSize(2);
    }

    @Test
    @DisplayName("searchWithPagination: 褁E��条件で検索できる")
    void testSearchWithPagination_MultipleConditions() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now());
        insertAuditLog("test-user@example.com", "test-admin@example.com", "CREATE", "顧客登録", LocalDateTime.now());

        // 検索: performedBy="admin" AND actionType="CREATE"
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            "admin", null, AuditLog.ActionType.CREATE, null, null, 10, 0, null, null
        );

        // 検証
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getPerformedBy()).isEqualTo("test-admin@example.com");
        assertThat(logs.get(0).getActionType()).isEqualTo(AuditLog.ActionType.CREATE);
    }

    @Test
    @DisplayName("countBySearch: 検索条件にマッチする件数を取得できる")
    void testCountBySearch() {
        // チE��トデータを挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now());
        insertAuditLog("test-user@example.com", "test-admin@example.com", "CREATE", "顧客登録", LocalDateTime.now());

        // 検索条件でカウンチE actionType="CREATE"
        long count = auditLogRepository.countBySearch(null, null, AuditLog.ActionType.CREATE, null, null);

        // 検証
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("searchWithPagination: 検索条件なし�E場合�E全件取得される")
    void testSearchWithPagination_NoConditions() {
        // チE��トデータめE件挿入
        insertAuditLog("test-admin@example.com", "test-user@example.com", "CREATE", "顧客登録", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "UPDATE", "顧客更新", LocalDateTime.now());
        insertAuditLog("test-admin@example.com", "test-user@example.com", "DELETE", "顧客削除", LocalDateTime.now());

        // 検索条件なぁE
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            null, null, null, null, null, 10, 0, null, null
        );

        // 検証: 全件取得される
        assertThat(logs).hasSize(3);
    }

    // ========================================
    // ヘルパ�EメソチE��
    // ========================================

    private void insertAuditLog(String performedBy, String targetEmail, String actionType, String actionDetail, LocalDateTime actionTime) {
        AuditLog auditLog = new AuditLog();
        auditLog.setPerformedBy(performedBy);
        auditLog.setTargetEmail(targetEmail);
        auditLog.setActionType(AuditLog.ActionType.valueOf(actionType));
        auditLog.setActionDetail(actionDetail);
        auditLog.setActionTime(actionTime);
        auditLog.setIpAddress("192.168.1.1");
        auditLogRepository.insert(auditLog);
    }
}
