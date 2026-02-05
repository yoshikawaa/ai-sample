package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.model.NotificationTypeCount;
import io.github.yoshikawaa.example.ai_sample.model.StatusCount;
import io.github.yoshikawaa.example.ai_sample.repository.NotificationHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest
@DisplayName("NotificationHistoryService のテスト")
class NotificationHistoryServiceTest {

    @Autowired
    private NotificationHistoryService notificationHistoryService;

    @MockitoBean
    private NotificationHistoryRepository notificationHistoryRepository;

    // テストデータ
    private final List<NotificationHistory> testNotifications = Arrays.asList(
        createTestNotification("user1@example.com", NotificationHistory.NotificationType.PASSWORD_RESET, NotificationHistory.Status.SUCCESS),
        createTestNotification("user2@example.com", NotificationHistory.NotificationType.ACCOUNT_LOCK, NotificationHistory.Status.FAILURE)
    );

    private NotificationHistory createTestNotification(String email, NotificationHistory.NotificationType type, NotificationHistory.Status status) {
        NotificationHistory notification = new NotificationHistory();
        notification.setRecipientEmail(email);
        notification.setNotificationType(type);
        notification.setStatus(status);
        notification.setSubject("テスト件名");
        notification.setBody("テスト本文");
        notification.setSentAt(LocalDateTime.now());
        return notification;
    }

    // ========================================
    // 履歴記録
    // ========================================

    @Nested
    @DisplayName("recordNotification: 通知履歴を記録")
    class RecordNotificationTest {

        @Test
        @DisplayName("成功した通知を記録できる")
        void testRecordNotification_Success() {
            notificationHistoryService.recordNotification(
                "test@example.com",
                NotificationHistory.NotificationType.PASSWORD_RESET,
                "Test Subject",
                "Test Body",
                true,
                null
            );

            verify(notificationHistoryRepository, times(1)).insert(any(NotificationHistory.class));
        }

        @Test
        @DisplayName("失敗した通知を記録できる")
        void testRecordNotification_Failure() {
            notificationHistoryService.recordNotification(
                "test@example.com",
                NotificationHistory.NotificationType.ACCOUNT_LOCK,
                "Test Subject",
                "Test Body",
                false,
                "メール送信に失敗しました"
            );

            verify(notificationHistoryRepository, times(1)).insert(any(NotificationHistory.class));
        }
    }

    // ========================================
    // 一覧・検索
    // ========================================

    @Nested
    @DisplayName("getAllNotificationHistoriesWithPagination: 全件取得（ページネーション対応）")
    class GetAllNotificationHistoriesWithPaginationTest {

        @Test
        @DisplayName("全件取得できる")
        void testGetAllNotificationHistoriesWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            
            NotificationHistory notification1 = new NotificationHistory();
            notification1.setRecipientEmail("user1@example.com");
            NotificationHistory notification2 = new NotificationHistory();
            notification2.setRecipientEmail("user2@example.com");
            
            when(notificationHistoryRepository.findAllWithPagination(10, 0, null, null))
                .thenReturn(Arrays.asList(notification1, notification2));
            when(notificationHistoryRepository.count()).thenReturn(2L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("ソート付きで取得できる")
        void testGetAllNotificationHistoriesWithPagination_WithSort() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("sentAt").descending());
            
            NotificationHistory notification1 = new NotificationHistory();
            notification1.setRecipientEmail("user1@example.com");
            
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "sent_at", "DESC"))
                .thenReturn(Arrays.asList(notification1));
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(notificationHistoryRepository, times(1)).findAllWithPagination(10, 0, "sent_at", "DESC");
        }
    }

    @Nested
    @DisplayName("searchNotificationHistoriesWithPagination: 検索（ページネーション対応）")
    class SearchNotificationHistoriesWithPaginationTest {

        @Test
        @DisplayName("検索条件で絞り込みできる")
        void testSearchNotificationHistoriesWithPagination() {
            NotificationHistorySearchForm searchForm = new NotificationHistorySearchForm();
            searchForm.setRecipientEmail("john");
            searchForm.setNotificationType(NotificationHistory.NotificationType.PASSWORD_RESET);
            searchForm.setStatus(NotificationHistory.Status.SUCCESS);
            
            Pageable pageable = PageRequest.of(0, 10);
            
            NotificationHistory notification = new NotificationHistory();
            notification.setRecipientEmail("john@example.com");
            
            when(notificationHistoryRepository.searchWithPagination(
                eq("john"),
                eq(NotificationHistory.NotificationType.PASSWORD_RESET),
                eq(NotificationHistory.Status.SUCCESS),
                any(),
                any(),
                eq(10),
                eq(0),
                any(),
                any()
            )).thenReturn(Arrays.asList(notification));
            
            when(notificationHistoryRepository.countBySearch(
                eq("john"),
                eq(NotificationHistory.NotificationType.PASSWORD_RESET),
                eq(NotificationHistory.Status.SUCCESS),
                any(),
                any()
            )).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.searchNotificationHistoriesWithPagination(searchForm, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("日付範囲で検索できる")
        void testSearchNotificationHistoriesWithPagination_WithDateRange() {
            NotificationHistorySearchForm searchForm = new NotificationHistorySearchForm();
            searchForm.setStartDate(LocalDate.of(2023, 1, 1));
            searchForm.setEndDate(LocalDate.of(2023, 12, 31));
            
            Pageable pageable = PageRequest.of(0, 10);
            
            when(notificationHistoryRepository.searchWithPagination(
                any(),
                any(),
                any(),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2024, 1, 1, 0, 0)),
                eq(10),
                eq(0),
                any(),
                any()
            )).thenReturn(Arrays.asList());
            
            when(notificationHistoryRepository.countBySearch(
                any(),
                any(),
                any(),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2024, 1, 1, 0, 0))
            )).thenReturn(0L);

            notificationHistoryService.searchNotificationHistoriesWithPagination(searchForm, pageable);

            verify(notificationHistoryRepository, times(1)).searchWithPagination(
                any(),
                any(),
                any(),
                eq(LocalDateTime.of(2023, 1, 1, 0, 0)),
                eq(LocalDateTime.of(2024, 1, 1, 0, 0)),
                eq(10),
                eq(0),
                any(),
                any()
            );
        }
    }

    // ========================================
    // 統計情報
    // ========================================

    @Nested
    @DisplayName("getStatistics: 統計情報を取得")
    class GetStatisticsTest {

        @Test
        @DisplayName("統計情報を取得できる")
        void testGetStatistics() {
            when(notificationHistoryRepository.count()).thenReturn(10L);
            
            StatusCount successCount = new StatusCount();
            successCount.setStatus("SUCCESS");
            successCount.setCount(8L);
            
            StatusCount failureCount = new StatusCount();
            failureCount.setStatus("FAILURE");
            failureCount.setCount(2L);
            
            when(notificationHistoryRepository.countByStatus())
                .thenReturn(Arrays.asList(successCount, failureCount));
            
            NotificationTypeCount typeCount = new NotificationTypeCount();
            typeCount.setNotificationType("PASSWORD_RESET");
            typeCount.setCount(5L);
            
            when(notificationHistoryRepository.countByNotificationType())
                .thenReturn(Arrays.asList(typeCount));

            NotificationHistoryService.NotificationHistoryStatistics statistics = notificationHistoryService.getStatistics();

            assertThat(statistics.getTotalCount()).isEqualTo(10L);
            assertThat(statistics.getSuccessCount()).isEqualTo(8L);
            assertThat(statistics.getFailureCount()).isEqualTo(2L);
            assertThat(statistics.getSuccessRate()).isEqualTo(80.0);
            assertThat(statistics.getTypeCounts()).isNotNull();
        }

        @Test
        @DisplayName("総送信数が0の場合、成功率は0.0になる")
        void testGetStatistics_TotalCountZero() {
            when(notificationHistoryRepository.count()).thenReturn(0L);
            when(notificationHistoryRepository.countByStatus()).thenReturn(Arrays.asList());
            when(notificationHistoryRepository.countByNotificationType()).thenReturn(Arrays.asList());

            NotificationHistoryService.NotificationHistoryStatistics statistics = notificationHistoryService.getStatistics();

            assertThat(statistics.getTotalCount()).isEqualTo(0L);
            assertThat(statistics.getSuccessCount()).isEqualTo(0L);
            assertThat(statistics.getFailureCount()).isEqualTo(0L);
            assertThat(statistics.getSuccessRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("extractSortInfo: ソート情報抽出")
    class ExtractSortInfoTest {

        @Test
        @DisplayName("recipientEmailプロパティをrecipient_emailカラムに変換できる")
        void testExtractSortInfo_RecipientEmail() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("recipientEmail").ascending());
            
            List<NotificationHistory> notifications = Arrays.asList(testNotifications.get(0));
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "recipient_email", "ASC"))
                .thenReturn(notifications);
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            verify(notificationHistoryRepository).findAllWithPagination(10, 0, "recipient_email", "ASC");
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("notificationTypeプロパティをnotification_typeカラムに変換できる")
        void testExtractSortInfo_NotificationType() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("notificationType").descending());
            
            List<NotificationHistory> notifications = Arrays.asList(testNotifications.get(0));
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "notification_type", "DESC"))
                .thenReturn(notifications);
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            verify(notificationHistoryRepository).findAllWithPagination(10, 0, "notification_type", "DESC");
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("subjectプロパティをsubjectカラムに変換できる")
        void testExtractSortInfo_Subject() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("subject").ascending());
            
            List<NotificationHistory> notifications = Arrays.asList(testNotifications.get(0));
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "subject", "ASC"))
                .thenReturn(notifications);
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            verify(notificationHistoryRepository).findAllWithPagination(10, 0, "subject", "ASC");
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("statusプロパティをstatusカラムに変換できる")
        void testExtractSortInfo_Status() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("status").ascending());
            
            List<NotificationHistory> notifications = Arrays.asList(testNotifications.get(0));
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "status", "ASC"))
                .thenReturn(notifications);
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            verify(notificationHistoryRepository).findAllWithPagination(10, 0, "status", "ASC");
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("createdAtプロパティをcreated_atカラムに変換できる")
        void testExtractSortInfo_CreatedAt() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
            
            List<NotificationHistory> notifications = Arrays.asList(testNotifications.get(0));
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "created_at", "ASC"))
                .thenReturn(notifications);
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            verify(notificationHistoryRepository).findAllWithPagination(10, 0, "created_at", "ASC");
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("未知のプロパティはsent_atにフォールバックする")
        void testExtractSortInfo_UnknownProperty() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("unknownProperty").ascending());
            
            List<NotificationHistory> notifications = Arrays.asList(testNotifications.get(0));
            when(notificationHistoryRepository.findAllWithPagination(10, 0, "sent_at", "ASC"))
                .thenReturn(notifications);
            when(notificationHistoryRepository.count()).thenReturn(1L);

            Page<NotificationHistory> result = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);

            verify(notificationHistoryRepository).findAllWithPagination(10, 0, "sent_at", "ASC");
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
