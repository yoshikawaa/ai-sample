package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistorySearchForm;
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

@WebMvcTest(AdminLoginHistoryController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("AdminLoginHistoryController のテスト")
class AdminLoginHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    // ========================================
    // ログイン履歴一覧表示
    // ========================================

    @Nested
    @DisplayName("showLoginHistory: ログイン履歴一覧表示")
    class ShowLoginHistoryTest {

        @Test
        @DisplayName("管理者はログイン履歴一覧を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowLoginHistory() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user1@example.com", "SUCCESS"),
                createLoginHistory(2L, "user2@example.com", "FAILURE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 2);

            // モックの動作を定義
            when(loginHistoryService.getAllLoginHistoriesWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"))
                .andExpect(model().attributeExists("loginHistorySearchForm"));

            // 検証
            verify(loginHistoryService, times(1)).getAllLoginHistoriesWithPagination(any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はページネーションでログイン履歴を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowLoginHistory_WithPagination() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(1, 10, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 20);

            // モックの動作を定義
            when(loginHistoryService.getAllLoginHistoriesWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).getAllLoginHistoriesWithPagination(any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はソート指定でログイン履歴を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowLoginHistory_WithSort() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("email").ascending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 1);

            // モックの動作を定義
            when(loginHistoryService.getAllLoginHistoriesWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history")
                    .param("sort", "email,asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).getAllLoginHistoriesWithPagination(any(Pageable.class));
        }

        @Test
        @DisplayName("ログイン履歴が0件の場合、空のページが表示される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowLoginHistory_EmptyPage() throws Exception {
            // テストデータ
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

            // モックの動作を定義
            when(loginHistoryService.getAllLoginHistoriesWithPagination(any(Pageable.class))).thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).getAllLoginHistoriesWithPagination(any(Pageable.class));
        }
    }

    // ========================================
    // ログイン履歴検索
    // ========================================

    @Nested
    @DisplayName("searchLoginHistory: ログイン履歴検索")
    class SearchLoginHistoryTest {

        @BeforeEach
        void setUpSearchTests() {
            // 検索用のデフォルトモック動作を設定
            List<LoginHistory> defaultHistories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable defaultPageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> defaultPage = new PageImpl<>(defaultHistories, defaultPageable, 1);
            
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(defaultPage);
        }

        @Test
        @DisplayName("管理者はメールアドレスでログイン履歴を検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_ByEmail() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 1);

            // モックの動作を定義
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はスタスでログイン履歴を検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_ByStatus() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user1@example.com", "SUCCESS"),
                createLoginHistory(2L, "user2@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 2);

            // モックの動作を定義
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者は日付範囲でログイン履歴を検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_ByDateRange() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 1);

            // モックの動作を定義
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("fromDate", "2024-01-01")
                    .param("toDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者はToDateのみでログイン履歴を検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_ByToDateOnly() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 1);

            // モックの動作を定義
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("toDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("管理者は複数条件でログイン履歴を検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_WithMultipleConditions() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 1);

            // モックの動作を定義
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("email", "user@example.com")
                    .param("status", "SUCCESS")
                    .param("fromDate", "2024-01-01")
                    .param("toDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("検索条件なしの場合、全件が返される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_NoConditions() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user1@example.com", "SUCCESS"),
                createLoginHistory(2L, "user2@example.com", "FAILURE")
            );
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 2);

            // モックの動作を定義（検索条件なしの場合は getAllLoginHistoriesWithPagination が呼ばれる）
            when(loginHistoryService.getAllLoginHistoriesWithPagination(any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).getAllLoginHistoriesWithPagination(any(Pageable.class));
            verify(loginHistoryService, never()).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("検索結果が0件の場合、空のページが表示される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_EmptyResult() throws Exception {
            // テストデータ
            Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

            // モックの動作を定義（検索条件ありなので searchLoginHistoriesWithPagination が呼ばれる）
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("email", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }

        @Test
        @DisplayName("検索結果をページネーションできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchLoginHistory_WithPagination() throws Exception {
            // テストデータ
            List<LoginHistory> histories = Arrays.asList(
                createLoginHistory(1L, "user@example.com", "SUCCESS")
            );
            Pageable pageable = PageRequest.of(1, 10, Sort.by("loginTime").descending());
            Page<LoginHistory> page = new PageImpl<>(histories, pageable, 20);

            // モックの動作を定義
            when(loginHistoryService.searchLoginHistoriesWithPagination(any(LoginHistorySearchForm.class), any(Pageable.class)))
                .thenReturn(page);

            // リクエストを実行
            mockMvc.perform(get("/admin/login-history/search")
                    .param("status", "SUCCESS")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-login-history"))
                .andExpect(model().attributeExists("historyPage"));

            // 検証
            verify(loginHistoryService, times(1)).searchLoginHistoriesWithPagination(
                any(LoginHistorySearchForm.class), any(Pageable.class));
        }
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private LoginHistory createLoginHistory(Long id, String email, String status) {
        LoginHistory history = new LoginHistory();
        history.setId(id);
        history.setEmail(email);
        history.setStatus(LoginHistory.Status.valueOf(status));
        history.setLoginTime(LocalDateTime.now());
        history.setIpAddress("192.168.1.1");
        history.setUserAgent("Mozilla/5.0");
        return history;
    }

    // ========================================
    // 認可制御（ロールごと）
    // ========================================

    @Nested
    @DisplayName("認可制御（ロールごと）")
    class AuthorizationTest {

        @Test
        @DisplayName("ADMINロールはログイン履歴にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessLoginHistory() throws Exception {
            when(loginHistoryService.getAllLoginHistoriesWithPagination(any())).thenReturn(new PageImpl<>(Collections.emptyList()));
            mockMvc.perform(get("/admin/login-history"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールはログイン履歴にアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessLoginHistory() throws Exception {
            mockMvc.perform(get("/admin/login-history"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証はログイン履歴にアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessLoginHistory() throws Exception {
            mockMvc.perform(get("/admin/login-history"))
                .andExpect(status().is3xxRedirection());
        }
    }
}

