package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.AuditLogSearchForm;
import io.github.yoshikawaa.example.ai_sample.service.AuditLogService;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminAuditLogController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("AdminAuditLogController のテスト")
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    // ========================================
    // 監査ログ一覧表示
    // ========================================

    @Nested
    @DisplayName("showAuditLog: 監査ログ一覧表示")
    class ShowAuditLogTest {

        @Test
        @DisplayName("管理者は監査ログ一覧を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowAuditLog() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE"),
                createAuditLog(2L, "admin@example.com", "user@example.com", "UPDATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 2);

            // モックの動作を定義
            when(auditLogService.getAllAuditLogsWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"))
                .andExpect(model().attributeExists("auditLogSearchForm"));

            // 検証
            verify(auditLogService, times(1)).getAllAuditLogsWithPagination(any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はページネーションで監査ログを表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowAuditLog_WithPagination() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(1, 10, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 20);

            // モックの動作を定義
            when(auditLogService.getAllAuditLogsWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).getAllAuditLogsWithPagination(any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はソート指定で監査ログを表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowAuditLog_WithSort() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("performedBy").ascending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.getAllAuditLogsWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log")
                    .param("sort", "performedBy,asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).getAllAuditLogsWithPagination(any(Pageable.class));
        }

        @Test
        @DisplayName("管理者は監査ログが0件の場合、空のページを表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowAuditLog_EmptyPage() throws Exception {
            // テストデータ
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

            // モックの動作を定義
            when(auditLogService.getAllAuditLogsWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).getAllAuditLogsWithPagination(any(Pageable.class));
        }
    }

    // ========================================
    // 監査ログ検索
    // ========================================

    @Nested
    @DisplayName("searchAuditLog: 監査ログ検索")
    class SearchAuditLogTest {

        @BeforeEach
        void setUpSearchTests() {
            // 検索用のデフォルトモック動作を設定
            List<AuditLog> defaultLogs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable defaultPageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> defaultPage = new PageImpl<>(defaultLogs, defaultPageable, 1);
            
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(defaultPage);
        }

        @Test
        @DisplayName("管理者は実行者で監査ログを検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_ByPerformedBy() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("performedBy", "admin@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者は対象メールで監査ログを検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_ByTargetEmail() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("targetEmail", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はアクションタイプで監査ログを検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_ByActionType() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("actionType", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者は日付範囲で監査ログを検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_ByDateRange() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("fromDate", "2024-01-01")
                    .param("toDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はToDateのみで監査ログを検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_ByToDateOnly() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("toDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者は複数条件で監査ログを検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_WithMultipleConditions() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("performedBy", "admin@example.com")
                    .param("actionType", "CREATE")
                    .param("fromDate", "2024-01-01")
                    .param("toDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("検索条件なしの場合、全件が返される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_NoConditions() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE"),
                createAuditLog(2L, "admin@example.com", "user@example.com", "UPDATE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 2);

            // モックの動作を定義（検索条件なしの場合は getAllAuditLogsWithPagination が呼ばれる）
            when(auditLogService.getAllAuditLogsWithPagination(any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).getAllAuditLogsWithPagination(any(Pageable.class));
            verify(auditLogService, never()).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("検索結果が0件の場合、空のページが表示される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_EmptyResult() throws Exception {
            // テストデータ
            Pageable pageable = PageRequest.of(0, 20, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

            // モックの動作を定義（検索条件ありなので searchAuditLogsWithPagination が呼ばれる）
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("performedBy", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("検索結果をページネーションできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchAuditLog_WithPagination() throws Exception {
            // テストデータ
            List<AuditLog> logs = Arrays.asList(
                createAuditLog(1L, "admin@example.com", "user@example.com", "CREATE")
            );
            Pageable pageable = PageRequest.of(1, 10, Sort.by("actionTime").descending());
            Page<AuditLog> page = new PageImpl<>(logs, pageable, 20);

            // モックの動作を定義
            when(auditLogService.searchAuditLogsWithPagination(any(AuditLogSearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/audit-log/search")
                    .param("actionType", "CREATE")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-audit-log"))
                .andExpect(model().attributeExists("logPage"));

            // 検証
            verify(auditLogService, times(1)).searchAuditLogsWithPagination(
                any(AuditLogSearchForm.class), any(Pageable.class));
        }
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

    // ========================================
    // 認可制御（ロールごと）
    // ========================================

    @Nested
    @DisplayName("認可制御（ロールごと）")
    class AuthorizationTest {

        @Test
        @DisplayName("ADMINロールは監査ログにアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessAuditLog() throws Exception {
            when(auditLogService.getAllAuditLogsWithPagination(any())).thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/admin/audit-log"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールは監査ログにアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessAuditLog() throws Exception {
            mockMvc.perform(get("/admin/audit-log"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証は監査ログにアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessAuditLog() throws Exception {
            mockMvc.perform(get("/admin/audit-log"))
                .andExpect(status().is3xxRedirection());
        }
    }
}

