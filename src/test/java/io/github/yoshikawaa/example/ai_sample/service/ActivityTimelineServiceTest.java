package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline;
import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline.ActivityType;
import io.github.yoshikawaa.example.ai_sample.repository.ActivityTimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@SpringBootTest
@DisplayName("ActivityTimelineService のテスト")
class ActivityTimelineServiceTest {

    @Autowired
    private ActivityTimelineService activityTimelineService;

    @MockitoBean
    private ActivityTimelineRepository activityTimelineRepository;

    private final String testEmail = "test@example.com";

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private ActivityTimeline createDto(Long id, LocalDateTime timestamp, ActivityType activityType, 
                                           String description, String detail, String ipAddress, String status) {
        return new ActivityTimeline(id, timestamp, activityType, description, detail, ipAddress, status);
    }

    @BeforeEach
    void setUp() {
        // モックのデフォルト動作を設定
        when(activityTimelineRepository.findActivityTimeline(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(activityTimelineRepository.countActivityTimeline(any(), any(), any(), any()))
            .thenReturn(0L);
    }

    // ========================================
    // 統合・フィルタ・変換・ページング等のテスト
    // ========================================

    @Test
    @DisplayName("3つのソースからデータを統合して取得できる")
    void testGetActivityTimeline_IntegratedData() {
        // モックデータを準備（データベースでソート済み：timestamp DESC）
        List<ActivityTimeline> dtos = Arrays.asList(
            createDto(3L, LocalDateTime.now(), ActivityType.NOTIFICATION_SENT, "通知送信: PASSWORD_RESET", "件名: パスワードリセット", null, "SUCCESS"),
            createDto(2L, LocalDateTime.now().minusDays(1), ActivityType.LOGIN_SUCCESS, "ログイン成功", null, "192.168.1.1", "SUCCESS"),
            createDto(1L, LocalDateTime.now().minusDays(2), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null)
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(null), eq(20), eq(0)))
            .thenReturn(dtos);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(null)))
            .thenReturn(3L);

        // テスト実行
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

        // 検証
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.NOTIFICATION_SENT);
        assertThat(result.getContent().get(1).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.LOGIN_SUCCESS);
        assertThat(result.getContent().get(2).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.ACCOUNT_CREATED);
    }

    @Test
    @DisplayName("指定された期間（過去30日）のデータを取得する")
    void testGetActivityTimeline_DefaultDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable = PageRequest.of(0, 20);

        activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

        // 検証: 開始日と終了日が30日前から今日まで
        verify(activityTimelineRepository).findActivityTimeline(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(null), eq(20), eq(0));
        verify(activityTimelineRepository).countActivityTimeline(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(null));
    }

    @Test
    @DisplayName("開始日と終了日を指定してフィルタリングできる")
    void testGetActivityTimeline_WithDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 20);

        activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

        // 検証: 指定した期間でリポジトリが呼ばれる
        verify(activityTimelineRepository).findActivityTimeline(eq(testEmail), any(LocalDateTime.class), any(LocalDateTime.class), eq(null), eq(20), eq(0));
    }

    @Test
    @DisplayName("データベース側でタイムスタンプ降順ソートされている")
    void testGetActivityTimeline_SortedByTimestampDesc() {
        // データベース側で既にソート済みのデータ
        List<ActivityTimeline> dtos = Arrays.asList(
            createDto(2L, LocalDateTime.now().minusDays(1), ActivityType.LOGIN_SUCCESS, "ログイン成功", null, "192.168.1.1", "SUCCESS"),
            createDto(3L, LocalDateTime.now().minusDays(2), ActivityType.NOTIFICATION_SENT, "通知送信: PASSWORD_RESET", "件名: パスワードリセット", null, "SUCCESS"),
            createDto(1L, LocalDateTime.now().minusDays(3), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null)
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(null), eq(20), eq(0)))
            .thenReturn(dtos);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(null)))
            .thenReturn(3L);

        // テスト実行
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable);

        // 検証: データベースから返された順序のまま
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getTimestamp()).isAfter(result.getContent().get(1).getTimestamp());
        assertThat(result.getContent().get(1).getTimestamp()).isAfter(result.getContent().get(2).getTimestamp());
    }

    @Test
    @DisplayName("ページネーションが正しく動作する")
    void testGetActivityTimeline_Pagination() {
        // 1ページ目（size=5）のデータ
        List<ActivityTimeline> page1Data = Arrays.asList(
            createDto(10L, LocalDateTime.now(), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(9L, LocalDateTime.now().minusDays(1), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(8L, LocalDateTime.now().minusDays(2), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(7L, LocalDateTime.now().minusDays(3), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(6L, LocalDateTime.now().minusDays(4), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null)
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(null), eq(5), eq(0)))
            .thenReturn(page1Data);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(null)))
            .thenReturn(10L);

        // 1ページ目
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable1 = PageRequest.of(0, 5);
        Page<ActivityTimeline> page1 = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, null, pageable1);

        // 検証
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(10);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.hasNext()).isTrue();

        // 2ページ目のデータ
        List<ActivityTimeline> page2Data = Arrays.asList(
            createDto(5L, LocalDateTime.now().minusDays(5), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(4L, LocalDateTime.now().minusDays(6), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(3L, LocalDateTime.now().minusDays(7), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(2L, LocalDateTime.now().minusDays(8), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null),
            createDto(1L, LocalDateTime.now().minusDays(9), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null)
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(null), eq(5), eq(5)))
            .thenReturn(page2Data);

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

    // ActivityTypeフィルタリング
    @Test
    @DisplayName("単一のActivityTypeでフィルタリングできる")
    void testGetActivityTimeline_SingleActivityTypeFilter() {
        // データベース側でフィルタリング済み
        List<ActivityTimeline> dtos = Arrays.asList(
            createDto(2L, LocalDateTime.now().minusDays(1), ActivityType.LOGIN_SUCCESS, "ログイン成功", null, "192.168.1.2", "SUCCESS")
        );

        List<ActivityTimeline.ActivityType> activityTypes = Arrays.asList(ActivityTimeline.ActivityType.LOGIN_SUCCESS);

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(activityTypes), eq(20), eq(0)))
            .thenReturn(dtos);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(activityTypes)))
            .thenReturn(1L);

        // テスト実行
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, activityTypes, pageable);

        // 検証
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActivityType()).isEqualTo(ActivityTimeline.ActivityType.LOGIN_SUCCESS);
    }

    @Test
    @DisplayName("複数のActivityTypeでフィルタリングできる")
    void testGetActivityTimeline_MultipleActivityTypeFilter() {
        // データベース側でフィルタリング済み
        List<ActivityTimeline> dtos = Arrays.asList(
            createDto(3L, LocalDateTime.now(), ActivityType.NOTIFICATION_SENT, "通知送信: PASSWORD_RESET", "件名: パスワードリセット", null, "SUCCESS"),
            createDto(2L, LocalDateTime.now().minusDays(1), ActivityType.LOGIN_SUCCESS, "ログイン成功", null, "192.168.1.1", "SUCCESS")
        );

        List<ActivityTimeline.ActivityType> activityTypes = Arrays.asList(
            ActivityTimeline.ActivityType.LOGIN_SUCCESS,
            ActivityTimeline.ActivityType.NOTIFICATION_SENT
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(activityTypes), eq(20), eq(0)))
            .thenReturn(dtos);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(activityTypes)))
            .thenReturn(2L);

        // テスト実行
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, activityTypes, pageable);

        // 検証
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("ActivityTypeフィルタがnullの場合は全件取得")
    void testGetActivityTimeline_NoActivityTypeFilter() {
        // フィルタなし
        List<ActivityTimeline> dtos = Arrays.asList(
            createDto(2L, LocalDateTime.now().minusDays(1), ActivityType.LOGIN_SUCCESS, "ログイン成功", null, "192.168.1.1", "SUCCESS"),
            createDto(1L, LocalDateTime.now().minusDays(2), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null)
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(null), eq(20), eq(0)))
            .thenReturn(dtos);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(null)))
            .thenReturn(2L);

        // テスト実行
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
        // 空リストはそのままリポジトリに渡されるが、MyBatisの動的SQLで空リストは無視される
        List<ActivityTimeline> dtos = Arrays.asList(
            createDto(2L, LocalDateTime.now().minusDays(1), ActivityType.LOGIN_SUCCESS, "ログイン成功", null, "192.168.1.1", "SUCCESS"),
            createDto(1L, LocalDateTime.now().minusDays(2), ActivityType.ACCOUNT_CREATED, "アカウント作成", "Test Detail", "192.168.1.1", null)
        );

        when(activityTimelineRepository.findActivityTimeline(eq(testEmail), any(), any(), eq(Collections.emptyList()), eq(20), eq(0)))
            .thenReturn(dtos);
        when(activityTimelineRepository.countActivityTimeline(eq(testEmail), any(), any(), eq(Collections.emptyList())))
            .thenReturn(2L);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityTimeline> result = activityTimelineService.getActivityTimeline(testEmail, startDate, endDate, Collections.emptyList(), pageable);
        assertThat(result.getContent()).hasSize(2);
    }
}
