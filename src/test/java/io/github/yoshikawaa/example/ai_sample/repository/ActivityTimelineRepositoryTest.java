package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline;
import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline.ActivityType;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ActivityTimelineRepository のテスト")
class ActivityTimelineRepositoryTest {

    @Autowired
    private ActivityTimelineRepository activityTimelineRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private final String testEmail = "timeline-test@example.com";
    private final LocalDateTime now = LocalDateTime.of(2026, 2, 5, 12, 0, 0);

    @BeforeEach
    void setUp() {
        // テストデータを投入
        
        // 外部キー制約対応: 顧客データを先に作成
        Customer customer = new Customer();
        customer.setEmail(testEmail);
        customer.setPassword("password");
        customer.setName("Timeline Test User");
        customer.setRegistrationDate(LocalDate.now());
        customer.setBirthDate(LocalDate.of(1990, 1, 1));
        customer.setPhoneNumber("000-0000-0000");
        customer.setAddress("Test Address");
        customer.setRole(Customer.Role.USER);
        customerRepository.insert(customer);
        
        // AuditLog: 3件
        AuditLog auditLog1 = new AuditLog();
        auditLog1.setPerformedBy("admin@example.com");
        auditLog1.setTargetEmail(testEmail);
        auditLog1.setActionType(AuditLog.ActionType.CREATE);
        auditLog1.setActionDetail("アカウント作成");
        auditLog1.setActionTime(now.minusDays(5));
        auditLog1.setIpAddress("192.168.1.1");
        auditLogRepository.insert(auditLog1);

        AuditLog auditLog2 = new AuditLog();
        auditLog2.setPerformedBy(testEmail);
        auditLog2.setTargetEmail(testEmail);
        auditLog2.setActionType(AuditLog.ActionType.UPDATE);
        auditLog2.setActionDetail("情報更新");
        auditLog2.setActionTime(now.minusDays(3));
        auditLog2.setIpAddress("192.168.1.2");
        auditLogRepository.insert(auditLog2);

        AuditLog auditLog3 = new AuditLog();
        auditLog3.setPerformedBy(testEmail);
        auditLog3.setTargetEmail(testEmail);
        auditLog3.setActionType(AuditLog.ActionType.PASSWORD_RESET);
        auditLog3.setActionDetail("パスワード変更");
        auditLog3.setActionTime(now.minusDays(1));
        auditLog3.setIpAddress("192.168.1.3");
        auditLogRepository.insert(auditLog3);

        // LoginHistory: 2件
        LoginHistory loginHistory1 = new LoginHistory();
        loginHistory1.setEmail(testEmail);
        loginHistory1.setStatus(LoginHistory.Status.SUCCESS);
        loginHistory1.setLoginTime(now.minusDays(4));
        loginHistory1.setIpAddress("192.168.1.10");
        loginHistory1.setUserAgent("Mozilla/5.0");
        loginHistoryRepository.insert(loginHistory1);

        LoginHistory loginHistory2 = new LoginHistory();
        loginHistory2.setEmail(testEmail);
        loginHistory2.setStatus(LoginHistory.Status.FAILURE);
        loginHistory2.setFailureReason("パスワード誤り");
        loginHistory2.setLoginTime(now.minusDays(2));
        loginHistory2.setIpAddress("192.168.1.11");
        loginHistory2.setUserAgent("Mozilla/5.0");
        loginHistoryRepository.insert(loginHistory2);

        // NotificationHistory: 2件
        NotificationHistory notification1 = new NotificationHistory();
        notification1.setRecipientEmail(testEmail);
        notification1.setNotificationType(NotificationHistory.NotificationType.ACCOUNT_LOCK);
        notification1.setSubject("アカウントロック通知");
        notification1.setBody("アカウントがロックされました");
        notification1.setSentAt(now.minusDays(6));
        notification1.setStatus(NotificationHistory.Status.SUCCESS);
        notificationHistoryRepository.insert(notification1);

        NotificationHistory notification2 = new NotificationHistory();
        notification2.setRecipientEmail(testEmail);
        notification2.setNotificationType(NotificationHistory.NotificationType.PASSWORD_RESET);
        notification2.setSubject("パスワードリセット");
        notification2.setBody("パスワードをリセットしてください");
        notification2.setSentAt(now);
        notification2.setStatus(NotificationHistory.Status.SUCCESS);
        notificationHistoryRepository.insert(notification2);
    }

    @Test
    @DisplayName("全アクティビティを統合して取得できる")
    void testFindActivityTimeline_AllActivities() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // 検証: 7件全て取得される（AuditLog3件 + LoginHistory2件 + NotificationHistory2件）
        assertThat(result).hasSize(7);
        
        // timestamp降順でソートされている
        assertThat(result.get(0).getTimestamp()).isEqualTo(now); // NotificationHistory2
        assertThat(result.get(1).getTimestamp()).isEqualTo(now.minusDays(1)); // AuditLog3
        assertThat(result.get(2).getTimestamp()).isEqualTo(now.minusDays(2)); // LoginHistory2
        assertThat(result.get(3).getTimestamp()).isEqualTo(now.minusDays(3)); // AuditLog2
        assertThat(result.get(4).getTimestamp()).isEqualTo(now.minusDays(4)); // LoginHistory1
        assertThat(result.get(5).getTimestamp()).isEqualTo(now.minusDays(5)); // AuditLog1
        assertThat(result.get(6).getTimestamp()).isEqualTo(now.minusDays(6)); // NotificationHistory1
    }

    @Test
    @DisplayName("ページネーションが正しく動作する")
    void testFindActivityTimeline_Pagination() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        // 1ページ目（limit=3, offset=0）
        List<ActivityTimeline> page1 = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 3, 0
        );
        assertThat(page1).hasSize(3);
        assertThat(page1.get(0).getTimestamp()).isEqualTo(now); // 最新

        // 2ページ目（limit=3, offset=3）
        List<ActivityTimeline> page2 = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 3, 3
        );
        assertThat(page2).hasSize(3);

        // 3ページ目（limit=3, offset=6）
        List<ActivityTimeline> page3 = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 3, 6
        );
        assertThat(page3).hasSize(1); // 残り1件
        assertThat(page3.get(0).getTimestamp()).isEqualTo(now.minusDays(6)); // 最古
    }

    @Test
    @DisplayName("日時範囲でフィルタリングできる")
    void testFindActivityTimeline_DateRangeFilter() {
        LocalDateTime startDateTime = now.minusDays(3);
        LocalDateTime endDateTime = now.minusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // 検証: 期間内の2件のみ（AuditLog3, LoginHistory2, AuditLog2）
        assertThat(result).hasSize(3);
        result.forEach(dto -> {
            assertThat(dto.getTimestamp()).isBetween(startDateTime, endDateTime);
        });
    }

    @Test
    @DisplayName("ActivityTypeでフィルタリングできる（単一）")
    void testFindActivityTimeline_SingleActivityTypeFilter() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);
        List<ActivityType> activityTypes = Arrays.asList(ActivityType.LOGIN_SUCCESS);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, activityTypes, 10, 0
        );

        // 検証: LOGIN_SUCCESSのみ
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityType()).isEqualTo(ActivityType.LOGIN_SUCCESS);
    }

    @Test
    @DisplayName("ActivityTypeでフィルタリングできる（複数）")
    void testFindActivityTimeline_MultipleActivityTypeFilter() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);
        List<ActivityType> activityTypes = Arrays.asList(ActivityType.LOGIN_SUCCESS, ActivityType.LOGIN_FAILURE);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, activityTypes, 10, 0
        );

        // 検証: LOGIN_SUCCESSとLOGIN_FAILUREのみ
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> 
            dto.getActivityType() == ActivityType.LOGIN_SUCCESS || dto.getActivityType() == ActivityType.LOGIN_FAILURE
        );
    }

    @Test
    @DisplayName("ActivityTypeフィルタがnullの場合は全件取得")
    void testFindActivityTimeline_NullActivityTypeFilter() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // 検証: 全件取得
        assertThat(result).hasSize(7);
    }

    @Test
    @DisplayName("ActivityTypeフィルタが空リストの場合は全件取得")
    void testFindActivityTimeline_EmptyActivityTypeFilter() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, Collections.emptyList(), 10, 0
        );

        // 検証: 全件取得
        assertThat(result).hasSize(7);
    }

    @Test
    @DisplayName("データがない場合は空リストを返す")
    void testFindActivityTimeline_NoData() {
        LocalDateTime startDateTime = now.minusDays(10);
        LocalDateTime endDateTime = now.minusDays(8);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // 検証: 空リスト
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("AuditLogのActivityTypeマッピングが正しい")
    void testFindActivityTimeline_AuditLogActivityTypeMapping() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // AuditLog由来のデータを検証
        ActivityTimeline createActivity = result.stream()
            .filter(dto -> dto.getActivityType() == ActivityType.ACCOUNT_CREATED)
            .findFirst()
            .orElseThrow();
        assertThat(createActivity.getDescription()).isEqualTo("アカウント作成");
        assertThat(createActivity.getDetail()).isEqualTo("アカウント作成");

        ActivityTimeline updateActivity = result.stream()
            .filter(dto -> dto.getActivityType() == ActivityType.INFO_UPDATED)
            .findFirst()
            .orElseThrow();
        assertThat(updateActivity.getDescription()).isEqualTo("情報更新");

        ActivityTimeline passwordResetActivity = result.stream()
            .filter(dto -> dto.getActivityType() == ActivityType.PASSWORD_RESET)
            .findFirst()
            .orElseThrow();
        assertThat(passwordResetActivity.getDescription()).isEqualTo("パスワードリセット");
    }

    @Test
    @DisplayName("LoginHistoryのActivityTypeマッピングが正しい")
    void testFindActivityTimeline_LoginHistoryActivityTypeMapping() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // LoginHistory由来のデータを検証
        ActivityTimeline successActivity = result.stream()
            .filter(dto -> dto.getActivityType() == ActivityType.LOGIN_SUCCESS)
            .findFirst()
            .orElseThrow();
        assertThat(successActivity.getDescription()).isEqualTo("ログイン成功");
        assertThat(successActivity.getStatus()).isEqualTo("SUCCESS");

        ActivityTimeline failureActivity = result.stream()
            .filter(dto -> dto.getActivityType() == ActivityType.LOGIN_FAILURE)
            .findFirst()
            .orElseThrow();
        assertThat(failureActivity.getDescription()).isEqualTo("ログイン失敗");
        assertThat(failureActivity.getDetail()).isEqualTo("失敗理由: パスワード誤り");
        assertThat(failureActivity.getStatus()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("NotificationHistoryのActivityTypeマッピングが正しい")
    void testFindActivityTimeline_NotificationHistoryActivityTypeMapping() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        List<ActivityTimeline> result = activityTimelineRepository.findActivityTimeline(
            testEmail, startDateTime, endDateTime, null, 10, 0
        );

        // NotificationHistory由来のデータを検証
        List<ActivityTimeline> notifications = result.stream()
            .filter(dto -> dto.getActivityType() == ActivityType.NOTIFICATION_SENT)
            .toList();
        assertThat(notifications).hasSize(2);
        
        ActivityTimeline notification1 = notifications.stream()
            .filter(dto -> dto.getDetail().contains("パスワードリセット"))
            .findFirst()
            .orElseThrow();
        assertThat(notification1.getDescription()).isEqualTo("通知送信: PASSWORD_RESET");
        assertThat(notification1.getDetail()).isEqualTo("件名: パスワードリセット");
        assertThat(notification1.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("総件数を正しく取得できる")
    void testCountActivityTimeline() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);

        long count = activityTimelineRepository.countActivityTimeline(
            testEmail, startDateTime, endDateTime, null
        );

        // 検証: 7件
        assertThat(count).isEqualTo(7);
    }

    @Test
    @DisplayName("総件数をActivityTypeフィルタ付きで取得できる")
    void testCountActivityTimeline_WithFilter() {
        LocalDateTime startDateTime = now.minusDays(7);
        LocalDateTime endDateTime = now.plusDays(1);
        List<ActivityType> activityTypes = Arrays.asList(ActivityType.LOGIN_SUCCESS, ActivityType.LOGIN_FAILURE);

        long count = activityTimelineRepository.countActivityTimeline(
            testEmail, startDateTime, endDateTime, activityTypes
        );

        // 検証: 2件（setUp()で作成したLOGIN_SUCCESSとLOGIN_FAILUREのデータ）
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("総件数を日時範囲フィルタ付きで取得できる")
    void testCountActivityTimeline_WithDateRangeFilter() {
        LocalDateTime startDateTime = now.minusDays(3);
        LocalDateTime endDateTime = now.minusDays(1);

        long count = activityTimelineRepository.countActivityTimeline(
            testEmail, startDateTime, endDateTime, null
        );

        // 検証: 3件（AuditLog3, LoginHistory2, AuditLog2）
        assertThat(count).isEqualTo(3);
    }
}
