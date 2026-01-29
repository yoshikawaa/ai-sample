package io.github.yoshikawaa.example.ai_sample.controller;


import io.github.yoshikawaa.example.ai_sample.service.AccountLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AccountUnlockController のテスト")
class AccountUnlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountLockService accountLockService;

    @Test
    @DisplayName("アンロックリクエスト画面が表示される")
    void testShowUnlockRequestForm() throws Exception {
        mockMvc.perform(get("/account-unlock/request"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-unlock-request"));
    }

    @Test
    @DisplayName("アンロックリクエストを送信すると申請完了画面に遷移")
    void testRequestUnlock() throws Exception {
        doNothing().when(accountLockService).requestUnlock(anyString(), anyString());
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", "test@example.com");
        params.add("name", "テストユーザー");
        mockMvc.perform(post("/account-unlock/request")
            .params(params)
            .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account-unlock/request-complete"));

        mockMvc.perform(get("/account-unlock/request-complete"))
            .andExpect(status().isOk())
            .andExpect(view().name("account-unlock-request-complete"));

        verify(accountLockService, times(1)).requestUnlock("test@example.com", "テストユーザー");
    }

    @Test
    @DisplayName("アンロック成功時は完了画面に遷移")
    void testUnlockAccountSuccess() throws Exception {
        when(accountLockService.unlockAccount("valid-token")).thenReturn(true);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "valid-token");
        mockMvc.perform(get("/account-unlock").params(params))
            .andExpect(status().isOk())
            .andExpect(view().name("account-unlock-complete"))
            .andExpect(model().attribute("success", true));
    }

    @Test
    @DisplayName("アンロック失敗時はエラー画面に遷移")
    void testUnlockAccountFailure() throws Exception {
        when(accountLockService.unlockAccount("invalid-token")).thenReturn(false);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", "invalid-token");
        mockMvc.perform(get("/account-unlock").params(params))
            .andExpect(status().isOk())
            .andExpect(view().name("account-unlock-complete"))
            .andExpect(model().attribute("success", false));
    }
}
