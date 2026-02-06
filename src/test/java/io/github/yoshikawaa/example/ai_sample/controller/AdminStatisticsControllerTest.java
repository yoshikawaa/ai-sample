package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.model.CustomerStatistics;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.LoginStatistics;
import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.StatisticsDto;
import io.github.yoshikawaa.example.ai_sample.model.UsageStatistics;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import io.github.yoshikawaa.example.ai_sample.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminStatisticsController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminStatisticsController のテスト")
class AdminStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @Test
    @DisplayName("統計画面を表示できる（管理者）")
    @WithMockUser(roles = "ADMIN")
    void testShowStatistics_AsAdmin() throws Exception {
        // モックデータ準備
        CustomerStatistics customerStat = new CustomerStatistics(LocalDate.of(2026, 1, 15), 10);
        LoginStatistics loginStat = new LoginStatistics(LoginHistory.Status.SUCCESS, 50);
        UsageStatistics usageStat = new UsageStatistics(AuditLog.ActionType.CREATE, 20);
        
        StatisticsDto mockData = new StatisticsDto(
            Arrays.asList(customerStat),
            Arrays.asList(loginStat),
            Arrays.asList(usageStat),
            LocalDate.now().minusDays(30),
            LocalDate.now()
        );
        
        when(statisticsService.getStatistics(any(), any()))
            .thenReturn(mockData);
        
        // 実行・検証
        mockMvc.perform(get("/admin/statistics"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin-statistics"))
            .andExpect(model().attributeExists("statisticsSearchForm"))
            .andExpect(model().attributeExists("statistics"));
        
        // サービスが呼ばれたことを検証（1メソッドのみ）
        verify(statisticsService, times(1)).getStatistics(any(), any());
    }

    @Test
    @DisplayName("期間指定で統計画面を表示できる（管理者）")
    @WithMockUser(roles = "ADMIN")
    void testSearchStatistics_AsAdmin() throws Exception {
        StatisticsDto mockData = new StatisticsDto(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31)
        );
        
        when(statisticsService.getStatistics(any(), any()))
            .thenReturn(mockData);
        
        mockMvc.perform(get("/admin/statistics/search")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin-statistics"))
            .andExpect(model().attributeExists("statistics"));
        
        verify(statisticsService, times(1)).getStatistics(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    }

    @Test
    @DisplayName("パラメータなしの場合はデフォルト期間（過去30日）で表示（@ModelAttributeメソッドで設定）")
    @WithMockUser(roles = "ADMIN")
    void testSearchStatistics_WithoutParams() throws Exception {
        StatisticsDto mockData = new StatisticsDto(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            LocalDate.now().minusDays(30),
            LocalDate.now()
        );
        
        when(statisticsService.getStatistics(any(), any()))
            .thenReturn(mockData);
        
        mockMvc.perform(get("/admin/statistics/search"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin-statistics"))
            .andExpect(model().attributeExists("statistics"));
        
        // サービスは呼ばれる（デフォルト期間でデータ取得）
        verify(statisticsService, times(1)).getStatistics(any(), any());
    }

    @Test
    @DisplayName("未認証ユーザーはアクセス不可（リダイレクト）")
    void testShowStatistics_Unauthenticated() throws Exception {
        mockMvc.perform(get("/admin/statistics"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("一般ユーザーはアクセス不可（403 Forbidden）")
    @WithMockUser(roles = "USER")
    void testShowStatistics_AsUser() throws Exception {
        mockMvc.perform(get("/admin/statistics"))
            .andExpect(status().isForbidden());
    }

    // ========================================
    // 認可制御（ロールごと）
    // ========================================

    @org.junit.jupiter.api.Nested
    @DisplayName("認可制御（ロールごと）")
    class AuthorizationTest {

        @Test
        @DisplayName("ADMINロールは統計画面にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessStatistics() throws Exception {
            StatisticsDto mockDto = new StatisticsDto(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDate.now().minusDays(30),
                LocalDate.now()
            );
            when(statisticsService.getStatistics(any(), any())).thenReturn(mockDto);
            mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールは統計画面にアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessStatistics() throws Exception {
            mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証は統計画面にアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessStatistics() throws Exception {
            mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().is3xxRedirection());
        }
    }
}
