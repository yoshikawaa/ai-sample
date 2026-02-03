package io.github.yoshikawaa.example.ai_sample.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionLimitExceededController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("SessionLimitExceededControllerのテスト")
class SessionLimitExceededControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @Test
    @DisplayName("/session-limit-exceededにアクセスするとエラー画面が表示される")
    void showSessionLimitExceeded() throws Exception {
        mockMvc.perform(get("/session-limit-exceeded"))
                .andExpect(status().isOk())
                .andExpect(view().name("session-limit-exceeded"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attributeExists("errorCode"));
    }
}
