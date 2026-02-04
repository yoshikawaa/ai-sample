package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerStatistics;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.LoginStatistics;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.UsageStatistics;
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
@DisplayName("StatisticsRepository のテスト")
class StatisticsRepositoryTest {

    @Autowired
    private StatisticsRepository statisticsRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        // 顧客データ投入（過去30日分）
        LocalDate baseDate = LocalDate.of(2026, 1, 15);
        Customer customer1 = new Customer("customer1@example.com", "password1", "Customer1", baseDate, LocalDate.of(1990, 1, 1), "090-1111-1111", "Address1", Customer.Role.USER);
        Customer customer2 = new Customer("customer2@example.com", "password2", "Customer2", baseDate, LocalDate.of(1991, 2, 2), "090-2222-2222", "Address2", Customer.Role.USER);
        Customer customer3 = new Customer("customer3@example.com", "password3", "Customer3", baseDate.plusDays(1), LocalDate.of(1992, 3, 3), "090-3333-3333", "Address3", Customer.Role.USER);
        Customer customer4 = new Customer("customer4@example.com", "password4", "Customer4", baseDate.plusDays(5), LocalDate.of(1993, 4, 4), "090-4444-4444", "Address4", Customer.Role.USER);
        customerRepository.insert(customer1);
        customerRepository.insert(customer2);
        customerRepository.insert(customer3);
        customerRepository.insert(customer4);
        
        // ログイン履歴データ投入
        loginHistoryRepository.insert(new LoginHistory(null, "customer1@example.com", LocalDateTime.of(2026, 1, 15, 10, 0), LoginHistory.Status.SUCCESS, null, "192.168.1.1", "Mozilla/5.0"));
        loginHistoryRepository.insert(new LoginHistory(null, "customer2@example.com", LocalDateTime.of(2026, 1, 15, 11, 0), LoginHistory.Status.SUCCESS, null, "192.168.1.2", "Mozilla/5.0"));
        loginHistoryRepository.insert(new LoginHistory(null, "customer3@example.com", LocalDateTime.of(2026, 1, 16, 10, 0), LoginHistory.Status.FAILURE, "Invalid credentials", "192.168.1.3", "Mozilla/5.0"));
        loginHistoryRepository.insert(new LoginHistory(null, "customer4@example.com", LocalDateTime.of(2026, 1, 20, 10, 0), LoginHistory.Status.LOCKED, "Account locked", "192.168.1.4", "Mozilla/5.0"));
        loginHistoryRepository.insert(new LoginHistory(null, "customer1@example.com", LocalDateTime.of(2026, 1, 21, 10, 0), LoginHistory.Status.SESSION_EXCEEDED, "Session limit exceeded", "192.168.1.1", "Mozilla/5.0"));
        
        // 監査ログデータ投入
        auditLogRepository.insert(new AuditLog(null, "admin@example.com", "customer1@example.com", AuditLog.ActionType.CREATE, "Created customer", LocalDateTime.of(2026, 1, 15, 9, 0), "192.168.1.100"));
        auditLogRepository.insert(new AuditLog(null, "admin@example.com", "customer2@example.com", AuditLog.ActionType.UPDATE, "Updated customer", LocalDateTime.of(2026, 1, 16, 9, 0), "192.168.1.100"));
        auditLogRepository.insert(new AuditLog(null, "admin@example.com", "customer3@example.com", AuditLog.ActionType.DELETE, "Deleted customer", LocalDateTime.of(2026, 1, 17, 9, 0), "192.168.1.100"));
        auditLogRepository.insert(new AuditLog(null, "admin@example.com", null, AuditLog.ActionType.VIEW_STATISTICS, "Viewed statistics", LocalDateTime.of(2026, 1, 18, 9, 0), "192.168.1.100"));
    }

    @Test
    @DisplayName("顧客数推移を取得できる（日別）")
    void testGetCustomerStatistics() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        List<CustomerStatistics> result = statisticsRepository.getCustomerStatistics(startDate, endDate);
        
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3); // 15日: 2件, 16日: 1件, 20日: 1件
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(result.get(0).getCount()).isEqualTo(2);
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2026, 1, 16));
        assertThat(result.get(1).getCount()).isEqualTo(1);
        assertThat(result.get(2).getDate()).isEqualTo(LocalDate.of(2026, 1, 20));
        assertThat(result.get(2).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("期間外のデータは除外される（顧客数推移）")
    void testGetCustomerStatistics_OutOfRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        
        List<CustomerStatistics> result = statisticsRepository.getCustomerStatistics(startDate, endDate);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ログイン統計を取得できる（ステータス別）")
    void testGetLoginStatistics() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        List<LoginStatistics> result = statisticsRepository.getLoginStatistics(startDate, endDate);
        
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(4); // SUCCESS, FAILURE, LOCKED, SESSION_EXCEEDED
        
        // SUCCESSの検証
        LoginStatistics successStat = result.stream()
            .filter(s -> s.getStatus() == LoginHistory.Status.SUCCESS)
            .findFirst()
            .orElseThrow();
        assertThat(successStat.getCount()).isEqualTo(2);
        
        // FAILUREの検証
        LoginStatistics failureStat = result.stream()
            .filter(s -> s.getStatus() == LoginHistory.Status.FAILURE)
            .findFirst()
            .orElseThrow();
        assertThat(failureStat.getCount()).isEqualTo(1);
        
        // LOCKEDの検証
        LoginStatistics lockedStat = result.stream()
            .filter(s -> s.getStatus() == LoginHistory.Status.LOCKED)
            .findFirst()
            .orElseThrow();
        assertThat(lockedStat.getCount()).isEqualTo(1);
        
        // SESSION_EXCEEDEDの検証
        LoginStatistics sessionStat = result.stream()
            .filter(s -> s.getStatus() == LoginHistory.Status.SESSION_EXCEEDED)
            .findFirst()
            .orElseThrow();
        assertThat(sessionStat.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("期間外のデータは除外される（ログイン統計）")
    void testGetLoginStatistics_OutOfRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        
        List<LoginStatistics> result = statisticsRepository.getLoginStatistics(startDate, endDate);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("利用状況統計を取得できる（アクションタイプ別）")
    void testGetUsageStatistics() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        List<UsageStatistics> result = statisticsRepository.getUsageStatistics(startDate, endDate);
        
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(4); // CREATE, UPDATE, DELETE, VIEW_STATISTICS
        
        // CREATEの検証
        UsageStatistics createStat = result.stream()
            .filter(s -> s.getActionType() == AuditLog.ActionType.CREATE)
            .findFirst()
            .orElseThrow();
        assertThat(createStat.getCount()).isEqualTo(1);
        
        // UPDATEの検証
        UsageStatistics updateStat = result.stream()
            .filter(s -> s.getActionType() == AuditLog.ActionType.UPDATE)
            .findFirst()
            .orElseThrow();
        assertThat(updateStat.getCount()).isEqualTo(1);
        
        // DELETEの検証
        UsageStatistics deleteStat = result.stream()
            .filter(s -> s.getActionType() == AuditLog.ActionType.DELETE)
            .findFirst()
            .orElseThrow();
        assertThat(deleteStat.getCount()).isEqualTo(1);
        
        // VIEW_STATISTICSの検証
        UsageStatistics viewStat = result.stream()
            .filter(s -> s.getActionType() == AuditLog.ActionType.VIEW_STATISTICS)
            .findFirst()
            .orElseThrow();
        assertThat(viewStat.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("期間外のデータは除外される（利用状況統計）")
    void testGetUsageStatistics_OutOfRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        
        List<UsageStatistics> result = statisticsRepository.getUsageStatistics(startDate, endDate);
        
        assertThat(result).isEmpty();
    }
}
