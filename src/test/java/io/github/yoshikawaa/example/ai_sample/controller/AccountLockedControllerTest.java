package io.github.yoshikawaa.example.ai_sample.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;

import static org.mockito.Mockito.when;

@WebMvcTest(AccountLockedController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("AccountLockedController のテスト")
class AccountLockedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @Test
    @WithMockUser
    @DisplayName("アカウントロック画面が表示される（lockedUntil値あり）")
    void showAccountLockedPage_lockedUntil() throws Exception {
        String email = "locked@example.com";
        String lockedUntil = "2026-01-24 12:34";
        when(loginAttemptService.getLockedUntilFormatted(email)).thenReturn(lockedUntil);

        mockMvc.perform(get("/account-locked").param("email", email))
            .andExpect(status().isOk())
            .andExpect(view().name("account-locked"))
            .andExpect(model().attribute("lockedUntil", lockedUntil));
    }

    @Test
    @WithMockUser
    @DisplayName("アカウントロック画面が表示される（lockedUntil値なし）")
    void showAccountLockedPage_noLockedUntil() throws Exception {
        String email = "notlocked@example.com";
        when(loginAttemptService.getLockedUntilFormatted(email)).thenReturn("");

        mockMvc.perform(get("/account-locked").param("email", email))
            .andExpect(status().isOk())
            .andExpect(view().name("account-locked"))
            .andExpect(model().attribute("lockedUntil", ""));
    }
}
