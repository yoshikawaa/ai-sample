package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.config.ActivityTimelineProperties;
import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline;
import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline.ActivityType;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.repository.AuditLogRepository;
import io.github.yoshikawaa.example.ai_sample.repository.LoginHistoryRepository;
import io.github.yoshikawaa.example.ai_sample.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * アクティビティタイムラインサービス
 * 顧客の活動履歴を統合的に管理
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ActivityTimelineService {

    private final AuditLogRepository auditLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final ActivityTimelineProperties activityTimelineProperties;

    /**
     * 顧客のアクティビティタイムラインを取得
     * 
     * @param email 顧客メールアドレス
     * @param startDate 検索期間開始日
     * @param endDate 検索期間終了日
     * @param activityTypes フィルタリング対象のアクティビティ種別（nullの場合は全種別）
     * @param pageable ページネーション情報
     * @return アクティビティタイムライン
     */
    public Page<ActivityTimeline> getActivityTimeline(String email, LocalDate startDate, LocalDate endDate, 
                                                       List<ActivityType> activityTypes, Pageable pageable) {
        log.info("アクティビティタイムライン取得開始: email={}, startDate={}, endDate={}", email, startDate, endDate);
        
        // LocalDateをLocalDateTimeに変換
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // 各履歴を取得（上限を設定して大量データを防ぐ）
        int fetchLimit = activityTimelineProperties.getFetchLimit();
        
        List<AuditLog> auditLogs = auditLogRepository.findByTargetEmail(email, startDateTime, endDateTime, fetchLimit);
        List<LoginHistory> loginHistories = loginHistoryRepository.findByEmail(email, startDateTime, endDateTime, fetchLimit);
        List<NotificationHistory> notifications = notificationHistoryRepository.findByRecipientEmail(
            email, startDateTime, endDateTime, fetchLimit);
        
        // ActivityTimelineに変換して統合
        List<ActivityTimeline> timeline = new ArrayList<>();
        timeline.addAll(convertAuditLogs(auditLogs));
        timeline.addAll(convertLoginHistories(loginHistories));
        timeline.addAll(convertNotifications(notifications));
        
        // 時系列ソート（最新が上）
        timeline.sort(Comparator.comparing(ActivityTimeline::getTimestamp).reversed());
        
        // フィルタリング（アクティビティ種別）
        if (!CollectionUtils.isEmpty(activityTypes)) {
            timeline = timeline.stream()
                .filter(a -> activityTypes.contains(a.getActivityType()))
                .collect(Collectors.toList());
        }
        
        // ページネーション適用
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), timeline.size());
        
        List<ActivityTimeline> pagedTimeline = timeline.subList(start, end);
        
        log.info("アクティビティタイムライン取得完了: email={}, 総件数={}, ページ件数={}", 
            email, timeline.size(), pagedTimeline.size());
        
        return new PageImpl<>(pagedTimeline, pageable, timeline.size());
    }
    
    /**
     * 監査ログをActivityTimelineに変換
     */
    private List<ActivityTimeline> convertAuditLogs(List<AuditLog> auditLogs) {
        return auditLogs.stream()
            .map(log -> {
                ActivityType activityType = mapAuditLogActionTypeToActivityType(log.getActionType());
                String description = activityType.getDisplayName();
                String detail = log.getActionDetail();
                
                return new ActivityTimeline(
                    log.getId(),
                    log.getActionTime(),
                    activityType,
                    description,
                    detail,
                    log.getIpAddress(),
                    null  // 監査ログにはステータスがない
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * ログイン履歴をActivityTimelineに変換
     */
    private List<ActivityTimeline> convertLoginHistories(List<LoginHistory> loginHistories) {
        return loginHistories.stream()
            .map(history -> {
                ActivityType activityType = mapLoginHistoryStatusToActivityType(history.getStatus());
                String description = activityType.getDisplayName();
                String detail = history.getFailureReason() != null 
                    ? "失敗理由: " + history.getFailureReason() 
                    : null;
                
                return new ActivityTimeline(
                    history.getId(),
                    history.getLoginTime(),
                    activityType,
                    description,
                    detail,
                    history.getIpAddress(),
                    history.getStatus().name()
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 通知履歴をActivityTimelineに変換
     */
    private List<ActivityTimeline> convertNotifications(List<NotificationHistory> notifications) {
        return notifications.stream()
            .map(notification -> {
                String description = "通知送信: " + notification.getNotificationType().name();
                String detail = "件名: " + notification.getSubject();
                if (StringUtils.hasText(notification.getErrorMessage())) {
                    detail += "\nエラー: " + notification.getErrorMessage();
                }
                
                return new ActivityTimeline(
                    notification.getId(),
                    notification.getSentAt(),
                    ActivityType.NOTIFICATION_SENT,
                    description,
                    detail,
                    null,  // 通知履歴にはIPアドレスがない
                    notification.getStatus().name()
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * AuditLog.ActionType を ActivityType にマッピング
     */
    private ActivityType mapAuditLogActionTypeToActivityType(AuditLog.ActionType actionType) {
        return switch (actionType) {
            case CREATE -> ActivityType.ACCOUNT_CREATED;
            case UPDATE -> ActivityType.INFO_UPDATED;
            case DELETE -> ActivityType.ACCOUNT_DELETED;
            case PASSWORD_RESET -> ActivityType.PASSWORD_RESET;
            case ACCOUNT_LOCK -> ActivityType.ACCOUNT_LOCKED;
            case ACCOUNT_UNLOCK -> ActivityType.ACCOUNT_UNLOCKED;
            case VIEW_STATISTICS -> ActivityType.INFO_UPDATED;  // デフォルトとしてINFO_UPDATEDにマッピング
        };
    }
    
    /**
     * LoginHistory.Status を ActivityType にマッピング
     */
    private ActivityType mapLoginHistoryStatusToActivityType(LoginHistory.Status status) {
        return switch (status) {
            case SUCCESS -> ActivityType.LOGIN_SUCCESS;
            case FAILURE -> ActivityType.LOGIN_FAILURE;
            case LOCKED -> ActivityType.ACCOUNT_LOCKED;
            case LOGOUT -> ActivityType.LOGOUT;
            case SESSION_EXCEEDED -> ActivityType.SESSION_EXCEEDED;
        };
    }
}
