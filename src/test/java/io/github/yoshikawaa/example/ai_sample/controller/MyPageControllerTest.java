package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("MyPageController のテスト")
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
    @DisplayName("GET /mypage: マイページを表示する")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testShowMyPage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/mypage"))
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("mypage")); // ビュー名が "mypage" であることを確認
    }

    @Test
    @DisplayName("GET /mypage/change-password: パスワード変更フォームを表示する")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testShowChangePasswordPage() throws Exception {
        // 正しいリクエストパスを使用してテスト実行
        mockMvc.perform(get("/mypage/change-password")) // クラスレベルとメソッドレベルのパスを合成
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("change-password")); // ビュー名が "change-password" であることを確認
    }

    @Test
    @DisplayName("POST /mypage/change-password: 正常にパスワードを変更する")
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
    @DisplayName("POST /mypage/change-password: バリデーションエラーの場合フォームを再表示する")
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
    @DisplayName("GET /mypage/change-password-complete: パスワード変更完了画面を表示する")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testShowChangePasswordCompletePage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/mypage/change-password-complete")) // 正しいリクエストパス
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("change-password-complete")); // ビュー名が "change-password-complete" であることを確認
    }

    @Test
    @DisplayName("GET /mypage/edit: 編集画面を表示できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void showEditPage() throws Exception {
        mockMvc.perform(get("/mypage/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-edit"))
            .andExpect(model().attributeExists("customerEditForm"))
            .andExpect(model().attribute("customerEditForm", org.hamcrest.Matchers.hasProperty("name", org.hamcrest.Matchers.is("Test User"))))
            .andExpect(model().attribute("customerEditForm", org.hamcrest.Matchers.hasProperty("phoneNumber", org.hamcrest.Matchers.is("123-456-7890"))));
    }

    @Test
    @DisplayName("POST /mypage/edit-confirm: 確認画面を表示できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void showEditConfirmPage() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Updated Name");
        params.add("birthDate", "1990-01-01");
        params.add("phoneNumber", "987-654-3210");
        params.add("address", "456 New St");

        mockMvc.perform(post("/mypage/edit-confirm")
                .params(params)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-edit-confirm"))
            .andExpect(model().attributeExists("customerEditForm"));
    }

    @Test
    @DisplayName("POST /mypage/edit: 確認画面から入力画面に戻れる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void backToEditPage() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Updated Name");
        params.add("birthDate", "1990-01-01");
        params.add("phoneNumber", "987-654-3210");
        params.add("address", "456 New St");

        mockMvc.perform(post("/mypage/edit")
                .params(params)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-edit"));
    }

    @Test
    @DisplayName("POST /mypage/update: 顧客情報を更新できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateCustomer() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Updated Name");
        params.add("birthDate", "1990-01-01");
        params.add("phoneNumber", "987-654-3210");
        params.add("address", "456 New St");

        mockMvc.perform(post("/mypage/update")
                .params(params)
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mypage/edit-complete"));

        verify(customerService, times(1)).updateCustomerInfo(any(Customer.class));
    }

    @Test
    @DisplayName("POST /mypage/update: バリデーションエラーで編集画面に戻る")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateCustomerWithValidationError() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "");  // 空の名前はバリデーションエラー
        params.add("birthDate", "");  // 空の生年月日もエラー
        params.add("phoneNumber", "");
        params.add("address", "");

        mockMvc.perform(post("/mypage/update")
                .params(params)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-edit"))
            .andExpect(model().attributeHasFieldErrors("customerEditForm", "name", "birthDate", "phoneNumber", "address"));

        verify(customerService, times(0)).updateCustomerInfo(any(Customer.class));
    }

    @Test
    @DisplayName("POST /mypage/edit-confirm: バリデーションエラーで編集画面に戻る")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void editConfirmWithValidationError() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "");  // 空の名前はバリデーションエラー
        params.add("birthDate", "1990-01-01");
        params.add("phoneNumber", "987-654-3210");
        params.add("address", "456 New St");

        mockMvc.perform(post("/mypage/edit-confirm")
                .params(params)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-edit"))
            .andExpect(model().attributeHasFieldErrors("customerEditForm", "name"));

        verify(customerService, times(0)).updateCustomerInfo(any(Customer.class));
    }

    @Test
    @DisplayName("GET /mypage/edit-complete: 更新完了画面を表示できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void showEditCompletePage() throws Exception {
        mockMvc.perform(get("/mypage/edit-complete"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-edit-complete"));
    }

    @Test
    @DisplayName("GET /mypage/delete: 削除確認画面を表示できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void showDeletePage() throws Exception {
        mockMvc.perform(get("/mypage/delete"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-delete"));
    }

    @Test
    @DisplayName("POST /mypage/delete-confirm: 削除最終確認画面を表示できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void showDeleteConfirmPage() throws Exception {
        mockMvc.perform(post("/mypage/delete-confirm")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-delete-confirm"));
    }

    @Test
    @DisplayName("POST /mypage/delete: 削除確認画面に戻れる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void handleBackToDelete() throws Exception {
        mockMvc.perform(post("/mypage/delete")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-delete"));
    }

    @Test
    @DisplayName("POST /mypage/delete-execute: 顧客を削除できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deleteCustomer() throws Exception {
        mockMvc.perform(post("/mypage/delete-execute")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mypage/delete-complete"));

        verify(customerService, times(1)).deleteCustomer("test@example.com");
    }

    @Test
    @DisplayName("GET /mypage/delete-complete: 削除完了画面を表示できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void showDeleteCompletePage() throws Exception {
        mockMvc.perform(get("/mypage/delete-complete"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer-delete-complete"));
    }
}
