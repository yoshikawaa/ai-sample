package io.github.yoshikawaa.example.ai_sample.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("AdminController の認可テスト")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @Nested
    @DisplayName("AuthorizationTest")
    class AuthorizationTest {

        @Test
        @DisplayName("管理者ユーザー: /admin/dashboard にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void accessAdminDashboard_withAdminRole() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));
        }

        @Test
        @DisplayName("一般ユーザー: /admin/dashboard にアクセスすると403 Forbidden")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void accessAdminDashboard_withUserRole() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証: /admin/dashboard にアクセスすると認証画面にリダイレクト")
        @WithAnonymousUser
        void accessAdminDashboard_anonymous() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
        }
    }
}
