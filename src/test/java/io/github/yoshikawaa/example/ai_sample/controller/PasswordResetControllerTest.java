package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.exception.InvalidTokenException;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import io.github.yoshikawaa.example.ai_sample.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(PasswordResetController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@WithMockUser
@DisplayName("PasswordResetController のテスト")
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @Test
    @DisplayName("GET /password-reset/request: リクエストフォーム画面を表示する")
    void testShowResetRequestForm() throws Exception {
        mockMvc.perform(get("/password-reset/request"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-request"));
    }

    @Test
    @DisplayName("POST /password-reset/request: 正常にリセットリンクを送信する")
    void testHandleResetRequest_正常系() throws Exception {
        // Arrange
        doNothing().when(passwordResetService).sendResetLink("test@example.com");

        // Act & Assert

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", "test@example.com");
        mockMvc.perform(post("/password-reset/request")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password-reset/request-complete"));

        mockMvc.perform(get("/password-reset/complete"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-complete"));

        verify(passwordResetService).sendResetLink("test@example.com");
    }

    @Test
    @DisplayName("GET /password-reset/complete: 完了画面を表示する")
    void testShowResetCompletePage() throws Exception {
        mockMvc.perform(get("/password-reset/complete"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-complete"));
    }

    @Test
    @DisplayName("GET /password-reset/confirm: 有効なトークンでフォーム画面を表示する")
    void testShowResetForm_有効なトークン() throws Exception {
        // Arrange
        doNothing().when(passwordResetService).validateResetToken("valid-token");

        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        mockMvc.perform(get("/password-reset/confirm")
                        .params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-form"))
                .andExpect(model().attributeExists("token"))
                .andExpect(model().attribute("token", "valid-token"));

        verify(passwordResetService).validateResetToken("valid-token");
    }

    @Test
    @DisplayName("GET /password-reset/confirm: 無効なトークンでエラー画面を表示する")
    void testShowResetForm_無効なトークン() throws Exception {
        // Arrange
        doThrow(new InvalidTokenException("無効または期限切れのトークンです。"))
                .when(passwordResetService).validateResetToken("invalid-token");

        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "invalid-token");
        mockMvc.perform(get("/password-reset/confirm")
                        .params(params))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("password-reset-error"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "無効または期限切れのトークンです。"));

        verify(passwordResetService).validateResetToken("invalid-token");
    }

    @Test
    @DisplayName("GET /password-reset/confirm: トークンがnullの場合、エラー画面を表示する")
    void testShowResetForm_トークンがNull() throws Exception {
        // Arrange
        doThrow(new InvalidTokenException("無効または期限切れのトークンです。"))
                .when(passwordResetService).validateResetToken(null);

        // Act & Assert
        mockMvc.perform(get("/password-reset/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("password-reset-error"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(passwordResetService).validateResetToken(null);
    }

    @Test
    @DisplayName("POST /password-reset/reset: 正常にパスワードをリセットする")
    void testResetPassword_正常系() throws Exception {
        // Arrange
        doNothing().when(passwordResetService).updatePassword("valid-token", "newPassword123");

        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        params.add("newPassword", "newPassword123");
        params.add("confirmPassword", "newPassword123");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password-reset/complete"));

        verify(passwordResetService).updatePassword("valid-token", "newPassword123");
    }

    @Test
    @DisplayName("POST /password-reset/reset: パスワードが短すぎる場合、バリデーションエラーを表示する")
    void testResetPassword_パスワードが短すぎる() throws Exception {
        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        params.add("newPassword", "12345");
        params.add("confirmPassword", "12345");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-form"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "newPassword"));

        verify(passwordResetService, never()).validateResetToken(anyString());
        verify(passwordResetService, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /password-reset/reset: パスワードが一致しない場合、バリデーションエラーを表示する")
    void testResetPassword_パスワードが一致しない() throws Exception {
        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        params.add("newPassword", "newPassword123");
        params.add("confirmPassword", "differentPassword");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-form"))
                .andExpect(model().attributeHasErrors("passwordResetForm"));

        verify(passwordResetService, never()).validateResetToken(anyString());
        verify(passwordResetService, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /password-reset/reset: パスワードが空の場合、バリデーションエラーを表示する")
    void testResetPassword_パスワードが空() throws Exception {
        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        params.add("newPassword", "");
        params.add("confirmPassword", "");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-form"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "newPassword"));

        verify(passwordResetService, never()).validateResetToken(anyString());
        verify(passwordResetService, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /password-reset/reset: 無効なトークンでエラー画面を表示する")
    void testResetPassword_無効なトークン() throws Exception {
        // Arrange
        doThrow(new InvalidTokenException("無効または期限切れのトークンです。"))
                .when(passwordResetService).updatePassword("invalid-token", "newPassword123");

        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "invalid-token");
        params.add("newPassword", "newPassword123");
        params.add("confirmPassword", "newPassword123");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("password-reset-error"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "無効または期限切れのトークンです。"));

        verify(passwordResetService).updatePassword("invalid-token", "newPassword123");
    }

    @Test
    @DisplayName("POST /password-reset/reset: 確認用パスワードが空の場合、バリデーションエラーを表示する")
    void testResetPassword_確認用パスワードが空() throws Exception {
        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        params.add("newPassword", "newPassword123");
        params.add("confirmPassword", "");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-form"))
                .andExpect(model().attributeHasFieldErrors("passwordResetForm", "confirmPassword"));

        verify(passwordResetService, never()).validateResetToken(anyString());
        verify(passwordResetService, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /password-reset/request-complete: リクエスト完了画面を表示する")
    void testShowResetRequestCompletePage() throws Exception {
        mockMvc.perform(get("/password-reset/request-complete"))
                .andExpect(status().isOk())
                .andExpect(view().name("password-reset-request-complete"));
    }

    @Test
    @DisplayName("InvalidTokenException が発生した場合、password-reset-error.html を表示する")
    void testHandleInvalidTokenException() throws Exception {
        // Arrange: updatePassword()内でInvalidTokenExceptionをスロー
        doThrow(new InvalidTokenException("無効または期限切れのトークンです。"))
                .when(passwordResetService).updatePassword("expiring-token", "newPassword123");

        // Act & Assert
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "expiring-token");
        params.add("newPassword", "newPassword123");
        params.add("confirmPassword", "newPassword123");
        mockMvc.perform(post("/password-reset/reset")
                        .params(params)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("password-reset-error"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "無効または期限切れのトークンです。"))
                .andExpect(model().attributeExists("errorCode"))
                .andExpect(model().attribute("errorCode", "400"));

        verify(passwordResetService).updatePassword("expiring-token", "newPassword123");
    }
}
