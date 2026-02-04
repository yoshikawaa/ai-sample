package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.CustomerStatistics;
import io.github.yoshikawaa.example.ai_sample.model.LoginStatistics;
import io.github.yoshikawaa.example.ai_sample.model.StatisticsDto;
import io.github.yoshikawaa.example.ai_sample.model.UsageStatistics;
import io.github.yoshikawaa.example.ai_sample.repository.StatisticsRepository;
import io.github.yoshikawaa.example.ai_sample.util.RequestContextUtil;
import io.github.yoshikawaa.example.ai_sample.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 統計サービス
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class StatisticsService {
    
    private final StatisticsRepository statisticsRepository;
    private final AuditLogService auditLogService;
    
    /**
     * すべての統計データを1トランザクションで取得し、アクセスログを記録
     * 
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 統計データ
     */
    public StatisticsDto getStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("統計データ取得: startDate={}, endDate={}", startDate, endDate);
        
        // 統計データ取得
        List<CustomerStatistics> customerStats = statisticsRepository.getCustomerStatistics(startDate, endDate);
        List<LoginStatistics> loginStats = statisticsRepository.getLoginStatistics(startDate, endDate);
        List<UsageStatistics> usageStats = statisticsRepository.getUsageStatistics(startDate, endDate);
        
        // 統計画面アクセスを記録
        String performedBy = SecurityContextUtil.getAuthenticatedUsername("unknown");
        String ipAddress = RequestContextUtil.getClientIpAddress();
        auditLogService.recordAudit(
            performedBy,
            null,
            AuditLog.ActionType.VIEW_STATISTICS,
            "統計画面アクセス",
            ipAddress
        );
        
        log.info("統計画面アクセス記録: performedBy={}, startDate={}, endDate={}", performedBy, startDate, endDate);
        
        return new StatisticsDto(customerStats, loginStats, usageStats, startDate, endDate);
    }
}
