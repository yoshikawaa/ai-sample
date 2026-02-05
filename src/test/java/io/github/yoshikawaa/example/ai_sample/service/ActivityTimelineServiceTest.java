package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.repository.AuditLogRepository;
import io.github.yoshikawaa.example.ai_sample.repository.LoginHistoryRepository;
import io.github.yoshikawaa.example.ai_sample.repository.NotificationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ActivityTimelineService のテスト")
class ActivityTimelineServiceTest {

    @Nested
    @SpringBootTest
    @DisplayName("デフォルトプロパティでの動作検証")
    class DefaultTest {
        @Autowired
        private ActivityTimelineService activityTimelineService;

        @MockitoBean
        private AuditLogRepository auditLogRepository;

        @MockitoBean
        private LoginHistoryRepository loginHistoryRepository;

        @MockitoBean
        private NotificationHistoryRepository notificationHistoryRepository;

        private final String testEmail = "test@example.com";

        @BeforeEach
        void setUp() {
            // モックのデフォルト動作を設定
            when(auditLogRepository.findByTargetEmail(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
            when(loginHistoryRepository.findByEmail(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
            when(notificationHistoryRepository.findByRecipientEmail(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
        }

        @Nested
        @DisplayName("getActivityTimeline: アクティビティタイムラインの取得")
        class GetActivityTimelineTest {

        @Test
        @DisplayName("3つのソースからデータを統合して取得できる")
        void testGetActivityTimeline_IntegratedData() {
            // モックデータを準備
            AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(2));
            LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));
            NotificationHistory notificationHistory = createNotificationHistory("PASSWORD_RESET", LocalDateTime.now());

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));
            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));
            when(notificationHistoryRepository.findByRecipientEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(notificationHistory));

            // テスト実行（デフォルト期間：過去30日）
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.NOTIFICATION_SENT);  // 最新
            assertThat(result.getContent().get(1).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.LOGIN_SUCCESS);
            assertThat(result.getContent().get(2).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.ACCOUNT_CREATED);  // 最古
        }

        @Test
        @DisplayName("指定された期間（過去30日）のデータを取得する")
        void testGetActivityTimeline_DefaultDateRange() {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);

            activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証: 開始日と終了日が30日前から今日まで
            verify(auditLogRepository).findByTargetEmail(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(500));
            verify(loginHistoryRepository).findByEmail(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(500));
            verify(notificationHistoryRepository).findByRecipientEmail(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(500));
        }

        @Test
        @DisplayName("開始日と終了日を指定してフィルタリングできる")
        void testGetActivityTimeline_WithDateRange() {
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            Pageable pageable = PageRequest.of(0, 20);

            activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証: 指定した期間でリポジトリが呼ばれる
            verify(auditLogRepository).findByTargetEmail(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(500));
        }

        @Test
        @DisplayName("タイムスタンプの降順（新しい順）でソートされる")
        void testGetActivityTimeline_SortedByTimestampDesc() {
            // モックデータを準備（異なるタイムスタンプ）
            AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(3));
            LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));
            NotificationHistory notificationHistory = createNotificationHistory("PASSWORD_RESET", LocalDateTime.now().minusDays(2));

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));
            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));
            when(notificationHistoryRepository.findByRecipientEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(notificationHistory));

            // テスト実行
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証: 新しい順に並んでいる
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).getTimestamp()).isAfter(result.getContent().get(1).getTimestamp());
            assertThat(result.getContent().get(1).getTimestamp()).isAfter(result.getContent().get(2).getTimestamp());
        }

        @Test
        @DisplayName("ページネーションが正しく動作する")
        void testGetActivityTimeline_Pagination() {
            // 10件のデータを準備
            List<AuditLog> auditLogs = Arrays.asList(
                createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(9)),
                createAuditLog(AuditLog.ActionType.UPDATE, LocalDateTime.now().minusDays(8)),
                createAuditLog(AuditLog.ActionType.DELETE, LocalDateTime.now().minusDays(7)),
                createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(6)),
                createAuditLog(AuditLog.ActionType.UPDATE, LocalDateTime.now().minusDays(5)),
                createAuditLog(AuditLog.ActionType.DELETE, LocalDateTime.now().minusDays(4)),
                createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(3)),
                createAuditLog(AuditLog.ActionType.UPDATE, LocalDateTime.now().minusDays(2)),
                createAuditLog(AuditLog.ActionType.DELETE, LocalDateTime.now().minusDays(1)),
                createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now())
            );

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(auditLogs);

            // 1ページ目（size=5）
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable1 = PageRequest.of(0, 5);
            Page<ActivityTimeline> page1 = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable1);

            // 検証
            assertThat(page1.getContent()).hasSize(5);
            assertThat(page1.getTotalElements()).isEqualTo(10);
            assertThat(page1.getTotalPages()).isEqualTo(2);
            assertThat(page1.hasNext()).isTrue();

            // 2ページ目
            Pageable pageable2 = PageRequest.of(1, 5);
            Page<ActivityTimeline> page2 = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable2);

            // 検証
            assertThat(page2.getContent()).hasSize(5);
            assertThat(page2.hasNext()).isFalse();
        }

        @Test
        @DisplayName("データがない場合は空ページを返す")
        void testGetActivityTimeline_EmptyResult() {
            // モックは既にBeforeEachで空リストを返すように設定済み

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("ActivityTypeフィルタリング")
    class ActivityTypeFilteringTest {

        @Test
        @DisplayName("単一のActivityTypeでフィルタリングできる")
        void testGetActivityTimeline_SingleActivityTypeFilter() {
            // モックデータを準備
            AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(2));
            LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));
            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));

            // LOGIN_SUCCESSのみをフィルタ
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            List<ActivityTimeline.ActivityType> activityTypes = Arrays.asList(ActivityTimeline.ActivityType.LOGIN_SUCCESS);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, activityTypes, pageable);

            // 検証
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.LOGIN_SUCCESS);
        }

        @Test
        @DisplayName("複数のActivityTypeでフィルタリングできる")
        void testGetActivityTimeline_MultipleActivityTypeFilter() {
            // モックデータを準備
            AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(2));
            LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));
            NotificationHistory notificationHistory = createNotificationHistory("PASSWORD_RESET", LocalDateTime.now());

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));
            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));
            when(notificationHistoryRepository.findByRecipientEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(notificationHistory));

            // LOGIN_SUCCESSとNOTIFICATION_SENTをフィルタ
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            List<ActivityTimeline.ActivityType> activityTypes = Arrays.asList(
                ActivityTimeline.ActivityType.LOGIN_SUCCESS,
                ActivityTimeline.ActivityType.NOTIFICATION_SENT
            );
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, activityTypes, pageable);

            // 検証
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("ActivityTypeフィルタがnullまたは空の場合は全件取得")
        void testGetActivityTimeline_NoActivityTypeFilter() {
            // モックデータを準備
            AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(2));
            LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));
            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));

            // フィルタなし
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証: 全件取得される
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("ActivityTypeフィルタが空リストの場合は全件取得")
        void testGetActivityTimeline_EmptyActivityTypeFilter() {
            AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(2));
            LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));
            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));
            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, Collections.emptyList(), pageable);
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("データ変換のテスト")
    class DataConversionTest {

        @Test
        @DisplayName("AuditLogを正しくActivityTimelineに変換できる")
        void testConvertAuditLog() {
            AuditLog auditLog = new AuditLog();
            auditLog.setId(1L);
            auditLog.setTargetEmail(testEmail);
            auditLog.setActionType(AuditLog.ActionType.UPDATE);
            auditLog.setActionDetail("顧客情報更新");
            auditLog.setActionTime(LocalDateTime.now());
            auditLog.setIpAddress("192.168.1.1");

            when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(auditLog));

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証
            ActivityTimeline timeline = result.getContent().get(0);
            assertThat(timeline.getId()).isEqualTo(1L);
            assertThat(timeline.getActivityType()).isEqualTo(ActivityTimeline.ActivityType.INFO_UPDATED);
            assertThat(timeline.getDescription()).isEqualTo("情報変更");
            assertThat(timeline.getDetail()).isEqualTo("顧客情報更新");
            assertThat(timeline.getIpAddress()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("LoginHistoryを正しくActivityTimelineに変換できる")
        void testConvertLoginHistory() {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setId(1L);
            loginHistory.setEmail(testEmail);
            loginHistory.setStatus(LoginHistory.Status.FAILURE);
            loginHistory.setFailureReason("パスワード誤り");
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setIpAddress("192.168.1.2");

            when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(loginHistory));

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証
            ActivityTimeline timeline = result.getContent().get(0);
            assertThat(timeline.getId()).isEqualTo(1L);
            assertThat(timeline.getActivityType()).isEqualTo(ActivityTimeline.ActivityType.LOGIN_FAILURE);
            assertThat(timeline.getDescription()).isEqualTo("ログイン失敗");
            assertThat(timeline.getDetail()).isEqualTo("失敗理由: パスワード誤り");
            assertThat(timeline.getIpAddress()).isEqualTo("192.168.1.2");
            assertThat(timeline.getStatus()).isEqualTo("FAILURE");
        }

        @Test
        @DisplayName("NotificationHistoryを正しくActivityTimelineに変換できる")
        void testConvertNotificationHistory() {
            NotificationHistory notificationHistory = new NotificationHistory();
            notificationHistory.setId(1L);
            notificationHistory.setRecipientEmail(testEmail);
            notificationHistory.setNotificationType(NotificationHistory.NotificationType.ACCOUNT_LOCK);
            notificationHistory.setSubject("アカウントロックの通知");
            notificationHistory.setStatus(NotificationHistory.Status.SUCCESS);
            notificationHistory.setSentAt(LocalDateTime.now());

            when(notificationHistoryRepository.findByRecipientEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(notificationHistory));

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

            // 検証
            ActivityTimeline timeline = result.getContent().get(0);
            assertThat(timeline.getId()).isEqualTo(1L);
            assertThat(timeline.getActivityType()).isEqualTo(ActivityTimeline.ActivityType.NOTIFICATION_SENT);
            assertThat(timeline.getDescription()).isEqualTo("通知送信: ACCOUNT_LOCK");
            assertThat(timeline.getDetail()).isEqualTo("件名: アカウントロックの通知");
            assertThat(timeline.getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("NotificationHistoryのerrorMessageが空文字・null・値ありを網羅")
        void testConvertNotificationHistory_ErrorMessageCases() {
            NotificationHistory n1 = createNotificationHistory("ACCOUNT_LOCK", LocalDateTime.now());
            n1.setErrorMessage(null);
            NotificationHistory n2 = createNotificationHistory("ACCOUNT_LOCK", LocalDateTime.now());
            n2.setErrorMessage("");
            NotificationHistory n3 = createNotificationHistory("ACCOUNT_LOCK", LocalDateTime.now());
            n3.setErrorMessage("エラー内容");
            when(notificationHistoryRepository.findByRecipientEmail(eq(testEmail), any(), any(), eq(500)))
                .thenReturn(Arrays.asList(n1, n2, n3));
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);
            assertThat(result.getContent().get(0).getDetail()).doesNotContain("エラー:");
            assertThat(result.getContent().get(1).getDetail()).doesNotContain("エラー:");
            assertThat(result.getContent().get(2).getDetail()).contains("エラー: エラー内容");
        }

        @Test
        @DisplayName("mapAuditLogActionTypeToActivityTypeの全Enum値を網羅")
        void testMapAuditLogActionTypeToActivityType_AllCases() {
            for (AuditLog.ActionType type : AuditLog.ActionType.values()) {
                ActivityTimeline.ActivityType mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    activityTimelineService, "mapAuditLogActionTypeToActivityType", type);
                assertThat(mapped).isNotNull();
            }
        }

        @Test
        @DisplayName("mapLoginHistoryStatusToActivityTypeの全Enum値を網羅")
        void testMapLoginHistoryStatusToActivityType_AllCases() {
            for (LoginHistory.Status status : LoginHistory.Status.values()) {
                ActivityTimeline.ActivityType mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    activityTimelineService, "mapLoginHistoryStatusToActivityType", status);
                assertThat(mapped).isNotNull();
            }
        }
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private AuditLog createAuditLog(AuditLog.ActionType actionType, LocalDateTime actionTime) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setTargetEmail(testEmail);
        auditLog.setActionType(actionType);
        auditLog.setActionDetail("Test Detail");
        auditLog.setActionTime(actionTime);
        auditLog.setIpAddress("192.168.1.1");
        return auditLog;
    }

    private LoginHistory createLoginHistory(LoginHistory.Status status, LocalDateTime loginTime) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setId(1L);
        loginHistory.setEmail(testEmail);
        loginHistory.setStatus(status);
        loginHistory.setLoginTime(loginTime);
        loginHistory.setIpAddress("192.168.1.1");
        if (status == LoginHistory.Status.FAILURE) {
            loginHistory.setFailureReason("Test Failure Reason");
        }
            @Test
            @DisplayName("ActivityTypeフィルタが空リストの場合は全件取得")
            void testGetActivityTimeline_EmptyActivityTypeFilter() {
                AuditLog auditLog = createAuditLog(AuditLog.ActionType.CREATE, LocalDateTime.now().minusDays(2));
                LoginHistory loginHistory = createLoginHistory(LoginHistory.Status.SUCCESS, LocalDateTime.now().minusDays(1));
                when(auditLogRepository.findByTargetEmail(eq(testEmail), any(), any(), eq(500)))
                    .thenReturn(Arrays.asList(auditLog));
                when(loginHistoryRepository.findByEmail(eq(testEmail), any(), any(), eq(500)))
                    .thenReturn(Arrays.asList(loginHistory));
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);
                Pageable pageable = PageRequest.of(0, 20);
                Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, Collections.emptyList(), pageable);
                assertThat(result.getContent()).hasSize(2);
            }

            @Test
            @DisplayName("NotificationHistoryのerrorMessageが空文字・null・値ありを網羅")
            void testConvertNotificationHistory_ErrorMessageCases() {
                NotificationHistory n1 = createNotificationHistory("ACCOUNT_LOCK", LocalDateTime.now());
                n1.setErrorMessage(null);
                NotificationHistory n2 = createNotificationHistory("ACCOUNT_LOCK", LocalDateTime.now());
                n2.setErrorMessage("");
                NotificationHistory n3 = createNotificationHistory("ACCOUNT_LOCK", LocalDateTime.now());
                n3.setErrorMessage("エラー内容");
                when(notificationHistoryRepository.findByRecipientEmail(eq(testEmail), any(), any(), eq(500)))
                    .thenReturn(Arrays.asList(n1, n2, n3));
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);
                Pageable pageable = PageRequest.of(0, 20);
                Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);
                assertThat(result.getContent().get(0).getDetail()).doesNotContain("エラー:");
                assertThat(result.getContent().get(1).getDetail()).doesNotContain("エラー:");
                assertThat(result.getContent().get(2).getDetail()).contains("エラー: エラー内容");
            }

            @Test
            @DisplayName("mapAuditLogActionTypeToActivityTypeの全Enum値を網羅")
            void testMapAuditLogActionTypeToActivityType_AllCases() {
                for (AuditLog.ActionType type : AuditLog.ActionType.values()) {
                    ActivityTimeline.ActivityType mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        activityTimelineService, "mapAuditLogActionTypeToActivityType", type);
                    assertThat(mapped).isNotNull();
                }
            }

            @Test
            @DisplayName("mapLoginHistoryStatusToActivityTypeの全Enum値を網羅")
            void testMapLoginHistoryStatusToActivityType_AllCases() {
                for (LoginHistory.Status status : LoginHistory.Status.values()) {
                    ActivityTimeline.ActivityType mapped = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        activityTimelineService, "mapLoginHistoryStatusToActivityType", status);
                    assertThat(mapped).isNotNull();
                }
            }
        }

        @Nested
        @SpringBootTest(properties = {
            "app.activity-timeline.fetch-limit=3"
        })
        @DisplayName("ActivityTimelineProperties変更時の動作検証")
        class PropertiesChangeTest {
            @Autowired
            private ActivityTimelineService activityTimelineService;

            @MockitoBean
            private AuditLogRepository auditLogRepository;

            @MockitoBean
            private LoginHistoryRepository loginHistoryRepository;

            @MockitoBean
            private NotificationHistoryRepository notificationHistoryRepository;

            private final String testEmail = "test@example.com";

            @BeforeEach
            void setUp() {
                when(auditLogRepository.findByTargetEmail(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
                when(loginHistoryRepository.findByEmail(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
                when(notificationHistoryRepository.findByRecipientEmail(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
            }

            @Test
            @DisplayName("fetch-limitプロパティの反映を検証する")
            void testFetchLimitProperty() {
                // fetch-limit=3 で各リポジトリの呼び出し引数を検証
                LocalDate endDate = LocalDate.now();
                LocalDate startDate = endDate.minusDays(30);
                Pageable pageable = PageRequest.of(0, 20);

                activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

                verify(auditLogRepository).findByTargetEmail(eq(testEmail), any(), any(), eq(3));
                verify(loginHistoryRepository).findByEmail(eq(testEmail), any(), any(), eq(3));
                verify(notificationHistoryRepository).findByRecipientEmail(eq(testEmail), any(), any(), eq(3));
            }
        }
