package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder; // PasswordEncoder を注入

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private CustomerService customerService;

    @BeforeEach
    void setUpCustomer() {
        // テスト用のユーザーをモック
        Customer testCustomer = new Customer();
        testCustomer.setEmail("test@example.com");
        testCustomer.setPassword(passwordEncoder.encode("password123")); // パスワードをエンコード
        testCustomer.setName("Test User");
        testCustomer.setBirthDate(LocalDate.of(1990, 1, 1));
        testCustomer.setPhoneNumber("123-456-7890");
        testCustomer.setAddress("123 Test St");
        testCustomer.setRegistrationDate(LocalDate.of(2023, 1, 1));

        // CustomerRepository の findByEmail メソッドをモック
        when(customerRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testCustomer));
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testShowMyPage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/mypage"))
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("mypage")); // ビュー名が "mypage" であることを確認
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testShowChangePasswordPage() throws Exception {
        // 正しいリクエストパスを使用してテスト実行
        mockMvc.perform(get("/mypage/change-password")) // クラスレベルとメソッドレベルのパスを合成
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("change-password")); // ビュー名が "change-password" であることを確認
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testChangePassword_ValidInput() throws Exception {
        // テストデータ（フォームデータ）
        MultiValueMap<String, String> passwordChangeForm = new LinkedMultiValueMap<>();
        passwordChangeForm.add("currentPassword", "password123"); // 現在のパスワード
        passwordChangeForm.add("newPassword", "newPassword456"); // 新しいパスワード
        passwordChangeForm.add("confirmPassword", "newPassword456"); // 新しいパスワードの確認
    
        // テスト実行
        mockMvc.perform(post("/mypage/change-password")
                        .params(passwordChangeForm) // フォームデータを送信
                        .with(csrf())) // CSRF トークンを送信
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/mypage/change-password-complete")); // リダイレクト先が "/mypage/change-password-complete" であることを確認
    
        // サービス呼び出しの検証
        verify(customerService, times(1)).changePassword(any(Customer.class), eq("newPassword456"));
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testChangePassword_InvalidInput() throws Exception {
        // テストデータ（フォームデータ: バリデーションエラーを意図的に発生させる）
        MultiValueMap<String, String> passwordChangeForm = new LinkedMultiValueMap<>();
        passwordChangeForm.add("currentPassword", "wrongPassword"); // 現在のパスワードが間違っている
        passwordChangeForm.add("newPassword", "short"); // 新しいパスワードが短すぎる
        passwordChangeForm.add("confirmPassword", "mismatch"); // 確認用パスワードが一致しない
    
        // テスト実行
        mockMvc.perform(post("/mypage/change-password")
                        .params(passwordChangeForm) // フォームデータを送信
                        .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // バリデーションエラー時はフォームを再表示するため 200 OK を期待
                .andExpect(view().name("change-password")) // ビュー名が "change-password" であることを確認
                .andExpect(model().attributeHasFieldErrors("changePasswordForm", "currentPassword", "newPassword", "confirmPassword")); // フィールドエラーを確認
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testShowChangePasswordCompletePage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/mypage/change-password-complete")) // 正しいリクエストパス
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("change-password-complete")); // ビュー名が "change-password-complete" であることを確認
    }
}