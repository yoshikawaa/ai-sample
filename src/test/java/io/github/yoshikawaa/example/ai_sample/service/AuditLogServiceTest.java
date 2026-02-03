package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.AuditLogSearchForm;
import io.github.yoshikawaa.example.ai_sample.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("AuditLogService のテスト")
class AuditLogServiceTest {

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    // ========================================
    // 監査ログ記録
    // ========================================

    @Test
    @DisplayName("recordAudit: 監査ログを記録できる")
    void testRecordAudit() {
        // モックの動作を定義
        doNothing().when(auditLogRepository).insert(any(AuditLog.class));

        // サービスメソッドを呼び出し
        auditLogService.recordAudit("admin@example.com", "user@example.com", AuditLog.ActionType.CREATE, "顧客登録", "192.168.1.1");

        // 検証
        verify(auditLogRepository, times(1)).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("recordAudit: 例外が発生してもログ記録は失敗しない（エラーログ出力のみ）")
    void testRecordAudit_ExceptionHandling() {
        // モックの動作を定義: 例外をスロー
        doThrow(new RuntimeException("Database error")).when(auditLogRepository).insert(any(AuditLog.class));

        // サービスメソッドを呼び出し（例外が発生しても正常終了する）
        auditLogService.recordAudit("admin@example.com", "user@example.com", AuditLog.ActionType.CREATE, "顧客登録", "192.168.1.1");

        // 検証: 呼び出しは成功する（例外がthrowされない）
        verify(auditLogRepository, times(1)).insert(any(AuditLog.class));
    }

    // ========================================
    // 全件取得+ページネーション
    // ========================================

    @Test
    @DisplayName("getAllAuditLogsWithPagination: ページネーションで全件取得できる")
    void testGetAllAuditLogsWithPagination() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE"),
            createAuditLog(2L, "admin@example.com", "user@example.com", "UPDATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findAllWithPagination(20, 0, null, null)).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<AuditLog> page = auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, null, null);
        verify(auditLogRepository, times(1)).count();
    }

    @Test
    @DisplayName("getAllAuditLogsWithPagination: ソート指定で取得できる")
    void testGetAllAuditLogsWithPagination_WithSort() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE"),
            createAuditLog(2L, "user@example.com", "admin@example.com", "UPDATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("performedBy").ascending());
        when(auditLogRepository.findAllWithPagination(20, 0, "performed_by", "ASC")).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<AuditLog> page = auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証
        assertThat(page.getContent()).hasSize(2);
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, "performed_by", "ASC");
    }

    @Test
    @DisplayName("getAllAuditLogsWithPagination: デフォルトソートが適用される")
    void testGetAllAuditLogsWithPagination_DefaultSort() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義（ソート指定なし）
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findAllWithPagination(20, 0, null, null)).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証: リポジトリにnullが渡される（リポジトリ側でデフォルトソート適用）
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, null, null);
    }

    // ========================================
    // 検索+ページネーション
    // ========================================

    @Test
    @DisplayName("searchAuditLogsWithPagination: 検索条件で絞り込みできる")
    void testSearchAuditLogsWithPagination() {
        // テストデータ
        AuditLogSearchForm searchForm = new AuditLogSearchForm();
        searchForm.setPerformedBy("admin@example.com");
        searchForm.setActionType(AuditLog.ActionType.CREATE);

        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.searchWithPagination(
            eq("admin@example.com"), any(), eq(AuditLog.ActionType.CREATE), any(), any(), eq(20), eq(0), any(), any()
        )).thenReturn(logs);
        when(auditLogRepository.countBySearch(
            eq("admin@example.com"), any(), eq(AuditLog.ActionType.CREATE), any(), any()
        )).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<AuditLog> page = auditLogService.searchAuditLogsWithPagination(searchForm, pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository, times(1)).searchWithPagination(
            eq("admin@example.com"), any(), eq(AuditLog.ActionType.CREATE), any(), any(), eq(20), eq(0), any(), any()
        );
        verify(auditLogRepository, times(1)).countBySearch(
            eq("admin@example.com"), any(), eq(AuditLog.ActionType.CREATE), any(), any()
        );
    }

    @Test
    @DisplayName("searchAuditLogsWithPagination: 日付範囲で検索できる")
    void testSearchAuditLogsWithPagination_WithDateRange() {
        // テストデータ
        AuditLogSearchForm searchForm = new AuditLogSearchForm();
        searchForm.setFromDate(LocalDate.of(2024, 1, 1));
        searchForm.setToDate(LocalDate.of(2024, 1, 31));

        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.searchWithPagination(
            any(), any(), any(), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), 
            anyInt(), anyInt(), any(), any()
        )).thenReturn(logs);
        when(auditLogRepository.countBySearch(
            any(), any(), any(), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31))
        )).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<AuditLog> page = auditLogService.searchAuditLogsWithPagination(searchForm, pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).searchWithPagination(
            any(), any(), any(), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), 
            anyInt(), anyInt(), any(), any()
        );
    }

    @Test
    @DisplayName("searchAuditLogsWithPagination: ソート指定で検索できる")
    void testSearchAuditLogsWithPagination_WithSort() {
        // テストデータ
        AuditLogSearchForm searchForm = new AuditLogSearchForm();
        searchForm.setActionType(AuditLog.ActionType.CREATE);

        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
        when(auditLogRepository.searchWithPagination(
            any(), any(), eq(AuditLog.ActionType.CREATE), any(), any(), eq(20), eq(0), eq("action_time"), eq("DESC")
        )).thenReturn(logs);
        when(auditLogRepository.countBySearch(
            any(), any(), eq(AuditLog.ActionType.CREATE), any(), any()
        )).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<AuditLog> page = auditLogService.searchAuditLogsWithPagination(searchForm, pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).searchWithPagination(
            any(), any(), eq(AuditLog.ActionType.CREATE), any(), any(), eq(20), eq(0), eq("action_time"), eq("DESC")
        );
    }

    // ========================================
    // プロパティ→カラムマッピング
    // ========================================

    @Test
    @DisplayName("getAllAuditLogsWithPagination: actionTypeプロパティがaction_typeカラムにマッピングされる")
    void testPropertyMapping_ActionType() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("actionType").ascending());
        when(auditLogRepository.findAllWithPagination(20, 0, "action_type", "ASC")).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<AuditLog> page = auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証: actionTypeがaction_typeにマッピングされる
        assertThat(page.getContent()).hasSize(1);
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, "action_type", "ASC");
    }

    @Test
    @DisplayName("getAllAuditLogsWithPagination: performedByプロパティがperformed_byカラムにマッピングされる")
    void testPropertyMapping_PerformedBy() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("performedBy").ascending());
        when(auditLogRepository.findAllWithPagination(20, 0, "performed_by", "ASC")).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証: performed_byカラムが使用される
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, "performed_by", "ASC");
    }

    @Test
    @DisplayName("getAllAuditLogsWithPagination: targetEmailプロパティがtarget_emailカラムにマッピングされる")
    void testPropertyMapping_TargetEmail() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("targetEmail").ascending());
        when(auditLogRepository.findAllWithPagination(20, 0, "target_email", "ASC")).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証: target_emailカラムが使用される
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, "target_email", "ASC");
    }

    @Test
    @DisplayName("getAllAuditLogsWithPagination: 不明なプロパティはaction_timeにフォールバックされる")
    void testPropertyMapping_Unknown() {
        // テストデータ
        List<AuditLog> logs = Arrays.asList(
            createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("unknownProperty").ascending());
        when(auditLogRepository.findAllWithPagination(20, 0, "action_time", "ASC")).thenReturn(logs);
        when(auditLogRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        auditLogService.getAllAuditLogsWithPagination(pageable);

        // 検証: action_timeカラムが使用される
        verify(auditLogRepository, times(1)).findAllWithPagination(20, 0, "action_time", "ASC");
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private AuditLog createAuditLog(Long id, String performedBy, String targetEmail, String actionType) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(id);
        auditLog.setPerformedBy(performedBy);
        auditLog.setTargetEmail(targetEmail);
        auditLog.setActionType(AuditLog.ActionType.valueOf(actionType));
        auditLog.setActionDetail("詳細");
        auditLog.setActionTime(LocalDateTime.now());
        auditLog.setIpAddress("192.168.1.1");
        return auditLog;
    }
}
