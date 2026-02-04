package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.StatisticsDto;
import io.github.yoshikawaa.example.ai_sample.repository.StatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("StatisticsService のテスト")
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;

    @MockitoBean
    private StatisticsRepository statisticsRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // モックの設定
        when(statisticsRepository.getCustomerStatistics(any(), any()))
            .thenReturn(Collections.emptyList());
        when(statisticsRepository.getLoginStatistics(any(), any()))
            .thenReturn(Collections.emptyList());
        when(statisticsRepository.getUsageStatistics(any(), any()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("統計データを取得できる（顧客数・ログイン・利用状況を一括取得）")
    @WithUserDetails(value = "admin@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testGetStatistics() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        StatisticsDto result = statisticsService.getStatistics(startDate, endDate);
        
        assertThat(result).isNotNull();
        assertThat(result.getCustomerStatistics()).isEmpty();
        assertThat(result.getLoginStatistics()).isEmpty();
        assertThat(result.getUsageStatistics()).isEmpty();
        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
        
        // リポジトリメソッドが呼ばれたことを検証
        verify(statisticsRepository, times(1)).getCustomerStatistics(startDate, endDate);
        verify(statisticsRepository, times(1)).getLoginStatistics(startDate, endDate);
        verify(statisticsRepository, times(1)).getUsageStatistics(startDate, endDate);
        
        // 監査ログが記録されたことを検証
        verify(auditLogService, times(1)).recordAudit(
            eq("admin@example.com"),
            eq(null),
            eq(AuditLog.ActionType.VIEW_STATISTICS),
            eq("統計画面アクセス"),
            any(String.class)
        );
    }
}
