package io.github.yoshikawaa.example.ai_sample.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.model.NotificationTypeCount;

import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import io.github.yoshikawaa.example.ai_sample.service.NotificationHistoryService;

@WebMvcTest(AdminNotificationHistoryController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminNotificationHistoryController のテスト")
class AdminNotificationHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationHistoryService notificationHistoryService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    private List<NotificationHistory> testHistories;
    private NotificationHistoryService.NotificationHistoryStatistics testStatistics;

    @BeforeEach
    void setUp() {
        testHistories = Arrays.asList(
            createHistory(1L, "user1@example.com", NotificationHistory.NotificationType.PASSWORD_RESET, 
                         "Password Reset", NotificationHistory.Status.SUCCESS, null),
            createHistory(2L, "user2@example.com", NotificationHistory.NotificationType.ACCOUNT_LOCK, 
                         "Account Locked", NotificationHistory.Status.SUCCESS, null),
            createHistory(3L, "user3@example.com", NotificationHistory.NotificationType.PASSWORD_RESET, 
                         "Password Reset", NotificationHistory.Status.FAILURE, "SMTP connection failed")
        );

        NotificationTypeCount typeCount1 = new NotificationTypeCount();
        typeCount1.setNotificationType("PASSWORD_RESET");
        typeCount1.setCount(50L);
        
        NotificationTypeCount typeCount2 = new NotificationTypeCount();
        typeCount2.setNotificationType("ACCOUNT_LOCK");
        typeCount2.setCount(30L);

        testStatistics = new NotificationHistoryService.NotificationHistoryStatistics(
            100L, 90L, 10L, 90.0,
            Arrays.asList(typeCount1, typeCount2)
        );
    }

    private NotificationHistory createHistory(Long id, String recipientEmail, 
                                             NotificationHistory.NotificationType type,
                                             String subject, NotificationHistory.Status status,
                                             String errorMessage) {
        NotificationHistory history = new NotificationHistory();
        history.setId(id);
        history.setRecipientEmail(recipientEmail);
        history.setNotificationType(type);
        history.setSubject(subject);
        history.setBody("Body text");
        history.setStatus(status);
        history.setErrorMessage(errorMessage);
        history.setSentAt(LocalDateTime.now());
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }

    @Nested
    @DisplayName("通知履歴一覧表示機能")
    class ShowNotificationHistoryTest {

        @Test
        @DisplayName("管理者は通知履歴一覧を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowNotificationHistory() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());

            when(notificationHistoryService.getAllNotificationHistoriesWithPagination(any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"))
                .andExpect(model().attributeExists("statistics"))
                .andExpect(model().attributeExists("notificationHistorySearchForm"));
        }

        @Test
        @DisplayName("認証なしではアクセスできない")
        void testShowNotificationHistory_Unauthorized() throws Exception {
            mockMvc.perform(get("/admin/notification-history"))
                .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("管理者以外はアクセスできない")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void testShowNotificationHistory_Forbidden() throws Exception {
            mockMvc.perform(get("/admin/notification-history"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("通知履歴検索機能")
    class SearchNotificationHistoryTest {

        @Test
        @DisplayName("管理者は通知履歴を検索できる（全条件指定）")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_AllCriteria() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationHistory> page = new PageImpl<>(testHistories.subList(0, 1), pageable, 1);
            
            NotificationHistorySearchForm searchForm = new NotificationHistorySearchForm();
            searchForm.setRecipientEmail("user1@example.com");
            searchForm.setNotificationType(NotificationHistory.NotificationType.PASSWORD_RESET);
            searchForm.setStatus(NotificationHistory.Status.SUCCESS);
            searchForm.setStartDate(LocalDate.of(2024, 1, 1));
            searchForm.setEndDate(LocalDate.of(2024, 12, 31));

            when(notificationHistoryService.searchNotificationHistoriesWithPagination(
                any(NotificationHistorySearchForm.class), 
                any(Pageable.class)
            )).thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history/search")
                    .param("recipientEmail", "user1@example.com")
                    .param("notificationType", "PASSWORD_RESET")
                    .param("status", "SUCCESS")
                    .param("startDate", "2024-01-01")
                    .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"))
                .andExpect(model().attributeExists("statistics"));
        }

        @Test
        @DisplayName("管理者は通知履歴を検索できる（メールアドレスのみ）")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_EmailOnly() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());
            
            NotificationHistorySearchForm searchForm = new NotificationHistorySearchForm();
            searchForm.setRecipientEmail("user@example.com");

            when(notificationHistoryService.searchNotificationHistoriesWithPagination(
                any(NotificationHistorySearchForm.class), 
                any(Pageable.class)
            )).thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history/search")
                    .param("recipientEmail", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }

        @Test
        @DisplayName("検索条件なしの場合は一覧表示にリダイレクト")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_NoCriteria() throws Exception {
            mockMvc.perform(get("/admin/notification-history/search"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/notification-history"));
        }

        @Test
        @DisplayName("通知種別のみで検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_NotificationTypeOnly() throws Exception {
            Pageable pageable = PageRequest.of(0, 20);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());

            when(notificationHistoryService.searchNotificationHistoriesWithPagination(any(), any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history/search")
                    .param("notificationType", "PASSWORD_RESET"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }

        @Test
        @DisplayName("ステータスのみで検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_StatusOnly() throws Exception {
            Pageable pageable = PageRequest.of(0, 20);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());

            when(notificationHistoryService.searchNotificationHistoriesWithPagination(any(), any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history/search")
                    .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }

        @Test
        @DisplayName("開始日のみで検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_StartDateOnly() throws Exception {
            Pageable pageable = PageRequest.of(0, 20);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());

            when(notificationHistoryService.searchNotificationHistoriesWithPagination(any(), any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history/search")
                    .param("startDate", "2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }

        @Test
        @DisplayName("終了日のみで検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchNotificationHistory_EndDateOnly() throws Exception {
            Pageable pageable = PageRequest.of(0, 20);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());

            when(notificationHistoryService.searchNotificationHistoriesWithPagination(any(), any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history/search")
                    .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }

        @Test
        @DisplayName("認証なしではアクセスできない")
        void testSearchNotificationHistory_Unauthorized() throws Exception {
            mockMvc.perform(get("/admin/notification-history/search")
                    .param("recipientEmail", "user@example.com"))
                .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("管理者以外はアクセスできない")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void testSearchNotificationHistory_Forbidden() throws Exception {
            mockMvc.perform(get("/admin/notification-history/search")
                    .param("recipientEmail", "user@example.com"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("ページネーション機能")
    class PaginationTest {

        @Test
        @DisplayName("ページ番号を指定して通知履歴を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowNotificationHistory_WithPage() throws Exception {
            Pageable pageable = PageRequest.of(1, 10);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, 30);

            when(notificationHistoryService.getAllNotificationHistoriesWithPagination(any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }

        @Test
        @DisplayName("ソート指定で通知履歴を表示できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowNotificationHistory_WithSort() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationHistory> page = new PageImpl<>(testHistories, pageable, testHistories.size());

            when(notificationHistoryService.getAllNotificationHistoriesWithPagination(any(Pageable.class)))
                .thenReturn(page);
            when(notificationHistoryService.getStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/admin/notification-history")
                    .param("sort", "sentAt,desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-notification-history"))
                .andExpect(model().attributeExists("historyPage"));
        }
    }
}
