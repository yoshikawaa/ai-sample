package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShowLoginPage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/login")) // GET リクエストを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("login")); // ビュー名が "login" であることを確認
    }
}
