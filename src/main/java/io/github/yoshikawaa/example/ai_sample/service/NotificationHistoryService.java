package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.model.NotificationTypeCount;
import io.github.yoshikawaa.example.ai_sample.model.StatusCount;
import io.github.yoshikawaa.example.ai_sample.repository.NotificationHistoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知履歴サービス
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class NotificationHistoryService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    // ========================================
    // 履歴記録
    // ========================================

    /**
     * 通知履歴を記録（別トランザクション）
     * メール送信の成功/失敗に関わらず、履歴を確実に記録する
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordNotification(String recipientEmail,
                                    NotificationHistory.NotificationType notificationType,
                                    String subject,
                                    String body,
                                    boolean success,
                                    String errorMessage) {
        NotificationHistory notification = new NotificationHistory();
        notification.setRecipientEmail(recipientEmail);
        notification.setNotificationType(notificationType);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setStatus(success ? NotificationHistory.Status.SUCCESS : NotificationHistory.Status.FAILURE);
        notification.setErrorMessage(errorMessage);
        notification.setSentAt(LocalDateTime.now());
        notification.setCreatedAt(LocalDateTime.now());

        notificationHistoryRepository.insert(notification);

        log.info("通知履歴記録: recipientEmail={}, notificationType={}, status={}",
            recipientEmail, notificationType, notification.getStatus());
    }

    // ========================================
    // 一覧・検索
    // ========================================

    /**
     * 全件取得（ページネーション対応）
     */
    @Transactional(readOnly = true)
    public Page<NotificationHistory> getAllNotificationHistoriesWithPagination(Pageable pageable) {
        String[] sortInfo = extractSortInfo(pageable);
        String sortColumn = sortInfo[0];
        String sortDirection = sortInfo[1];

        List<NotificationHistory> notifications = notificationHistoryRepository.findAllWithPagination(
            pageable.getPageSize(),
            (int) pageable.getOffset(),
            sortColumn,
            sortDirection
        );
        long total = notificationHistoryRepository.count();

        return new PageImpl<>(notifications, pageable, total);
    }

    /**
     * 検索（ページネーション対応）
     */
    @Transactional(readOnly = true)
    public Page<NotificationHistory> searchNotificationHistoriesWithPagination(NotificationHistorySearchForm searchForm,
                                                                                 Pageable pageable) {
        String recipientEmail = searchForm.getRecipientEmail();
        NotificationHistory.NotificationType notificationType = searchForm.getNotificationType();
        NotificationHistory.Status status = searchForm.getStatus();
        LocalDate startDate = searchForm.getStartDate();
        LocalDate endDate = searchForm.getEndDate();

        // LocalDate → LocalDateTime変換
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        String[] sortInfo = extractSortInfo(pageable);
        String sortColumn = sortInfo[0];
        String sortDirection = sortInfo[1];

        List<NotificationHistory> notifications = notificationHistoryRepository.searchWithPagination(
            recipientEmail,
            notificationType,
            status,
            startDateTime,
            endDateTime,
            pageable.getPageSize(),
            (int) pageable.getOffset(),
            sortColumn,
            sortDirection
        );
        long total = notificationHistoryRepository.countBySearch(
            recipientEmail,
            notificationType,
            status,
            startDateTime,
            endDateTime
        );

        return new PageImpl<>(notifications, pageable, total);
    }

    // ========================================
    // 統計情報
    // ========================================

    /**
     * 統計情報を取得
     */
    @Transactional(readOnly = true)
    public NotificationHistoryStatistics getStatistics() {
        // 総送信数
        long totalCount = notificationHistoryRepository.count();

        // ステータスごとの集計
        List<StatusCount> statusCounts = notificationHistoryRepository.countByStatus();
        long successCount = statusCounts.stream()
            .filter(c -> "SUCCESS".equals(c.getStatus()))
            .mapToLong(StatusCount::getCount)
            .sum();
        long failureCount = statusCounts.stream()
            .filter(c -> "FAILURE".equals(c.getStatus()))
            .mapToLong(StatusCount::getCount)
            .sum();

        double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0.0;

        // 通知種別ごとの集計
        List<NotificationTypeCount> typeCounts = notificationHistoryRepository.countByNotificationType();

        return new NotificationHistoryStatistics(totalCount, successCount, failureCount, successRate, typeCounts);
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    /**
     * Pageableからソート情報を抽出
     */
    private String[] extractSortInfo(Pageable pageable) {
        String sortColumn = null;
        String sortDirection = null;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            sortDirection = order.getDirection().isAscending() ? "ASC" : "DESC";

            // プロパティ名をカラム名に変換
            sortColumn = switch (property) {
                case "recipientEmail" -> "recipient_email";
                case "notificationType" -> "notification_type";
                case "subject" -> "subject";
                case "status" -> "status";
                case "sentAt" -> "sent_at";
                case "createdAt" -> "created_at";
                default -> "sent_at";
            };
        }

        return new String[]{sortColumn, sortDirection};
    }

    // ========================================
    // 統計情報クラス
    // ========================================

    /**
     * 統計情報クラス
     */
    @Data
    public static class NotificationHistoryStatistics {
        private final long totalCount;
        private final long successCount;
        private final long failureCount;
        private final double successRate;
        private final List<NotificationTypeCount> typeCounts;
    }
}
