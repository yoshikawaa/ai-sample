package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.AuditLogSearchForm;
import io.github.yoshikawaa.example.ai_sample.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 監査ログを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAudit(String performedBy, String targetEmail, AuditLog.ActionType actionType, String actionDetail, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setPerformedBy(performedBy);
            auditLog.setTargetEmail(targetEmail);
            auditLog.setActionType(actionType);
            auditLog.setActionDetail(actionDetail);
            auditLog.setActionTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);

            auditLogRepository.insert(auditLog);
            log.info("監査ログを記録: performedBy={}, targetEmail={}, actionType={}", performedBy, targetEmail, actionType);
        } catch (Exception e) {
            log.error("監査ログの記録に失敗: performedBy={}, targetEmail={}, actionType={}", performedBy, targetEmail, actionType, e);
        }
    }

    /**
     * 監査ログを取得（ページネーション対応）
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllAuditLogsWithPagination(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<AuditLog> logs = auditLogRepository.findAllWithPagination(pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = auditLogRepository.count();
        return new PageImpl<>(logs, pageable, total);
    }

    /**
     * 監査ログを検索（ページネーション対応）
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogsWithPagination(AuditLogSearchForm searchForm, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        
        List<AuditLog> logs = auditLogRepository.searchWithPagination(
            searchForm.getPerformedBy(),
            searchForm.getTargetEmail(),
            searchForm.getActionType(),
            searchForm.getFromDate(),
            searchForm.getToDate(),
            pageSize,
            offset,
            sortInfo[0],
            sortInfo[1]
        );
        
        long total = auditLogRepository.countBySearch(
            searchForm.getPerformedBy(),
            searchForm.getTargetEmail(),
            searchForm.getActionType(),
            searchForm.getFromDate(),
            searchForm.getToDate()
        );
        
        return new PageImpl<>(logs, pageable, total);
    }

    /**
     * Pageableからソートカラムとソート方向を抽出
     */
    private String[] extractSortInfo(Pageable pageable) {
        String[] sortInfo = new String[2];
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            String direction = order.getDirection().name();
            
            String column = switch (property) {
                case "performedBy" -> "performed_by";
                case "targetEmail" -> "target_email";
                case "actionType" -> "action_type";
                case "actionTime" -> "action_time";
                default -> "action_time";
            };
            
            sortInfo[0] = column;
            sortInfo[1] = direction;
        }
        return sortInfo;
    }
}
