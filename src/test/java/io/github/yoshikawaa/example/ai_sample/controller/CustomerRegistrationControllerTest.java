package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerRegistrationController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("CustomerRegistrationController のテスト")
class CustomerRegistrationControllerTest {

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /register/input: 顧客入力フォームを表示する")
    void testShowInputForm() throws Exception {
        // テスト実行
        mockMvc.perform(get("/register/input"))
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")); // ビュー名が "customer-input" であることを確認
    }

    @Test
    @DisplayName("POST /customers/confirm: 正常な入力で確認画面を表示する")
    void testShowConfirmForm_ValidInput() throws Exception {
        // テストデータ（バリデーションエラーなし）
        MultiValueMap<String, String> validCustomerForm = new LinkedMultiValueMap<>();
        validCustomerForm.add("email", "valid.email@example.com");
        validCustomerForm.add("password", "password123");
        validCustomerForm.add("confirmPassword", "password123");
        validCustomerForm.add("name", "Valid User");
        validCustomerForm.add("birthDate", "1990-05-20");
        validCustomerForm.add("phoneNumber", "123-456-7890");
        validCustomerForm.add("address", "123 Main St");

        // テスト実行
        mockMvc.perform(post("/register/confirm")
                .params(validCustomerForm) // params を使用してフォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-confirm")) // ビュー名が "customer-confirm" であることを確認
                .andExpect(model().attributeHasNoErrors("customerForm")); // バリデーションエラーがないことを確認
        }

    @Test
    @DisplayName("POST /customers/confirm: 不正な入力で入力画面に戻る")
    void testShowConfirmForm_InvalidInput() throws Exception {
        // テストデータ（バリデーションエラーあり）
        MultiValueMap<String, String> invalidCustomerForm = new LinkedMultiValueMap<>();
        invalidCustomerForm.add("email", ""); // 空の値でバリデーションエラーを発生させる
        invalidCustomerForm.add("password", "short");
        invalidCustomerForm.add("confirmPassword", "mismatch");
        invalidCustomerForm.add("name", "");
        invalidCustomerForm.add("birthDate", "");
        invalidCustomerForm.add("phoneNumber", "");
        invalidCustomerForm.add("address", "");

        // テスト実行
        mockMvc.perform(post("/register/confirm")
                .params(invalidCustomerForm) // params を使用してフォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")) // ビュー名が "customer-input" に戻ることを確認
                .andExpect(model().attributeHasFieldErrors("customerForm", "email", "name", "password", "confirmPassword", "birthDate", "phoneNumber", "address")); // バリデーションエラーを確認
    }

    @Test
    @DisplayName("POST /customers/input: 入力画面に戻る処理")
    void testHandleBackToInput() throws Exception {
        // テストデータ
        MultiValueMap<String, String> customerForm = new LinkedMultiValueMap<>();
        customerForm.add("email", "test@example.com");
        customerForm.add("password", "password123");
        customerForm.add("confirmPassword", "password123");
        customerForm.add("name", "Test User");
        customerForm.add("birthDate", "1990-01-01");
        customerForm.add("phoneNumber", "123-456-7890");
        customerForm.add("address", "123 Test St");

        // テスト実行
        mockMvc.perform(post("/register/input")
                .params(customerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")); // ビュー名が "customer-input" であることを確認
    }

    @Test
    @DisplayName("POST /customers/register: 正常に顧客を登録する")
    void testRegisterCustomer_ValidInput() throws Exception {
        // テストデータ（バリデーションエラーなし）
        MultiValueMap<String, String> validCustomerForm = new LinkedMultiValueMap<>();
        validCustomerForm.add("email", "valid.email@example.com");
        validCustomerForm.add("password", "password123");
        validCustomerForm.add("confirmPassword", "password123");
        validCustomerForm.add("name", "Valid User");
        validCustomerForm.add("birthDate", "1990-05-20");
        validCustomerForm.add("phoneNumber", "123-456-7890");
        validCustomerForm.add("address", "123 Main St");

        // テスト実行
        mockMvc.perform(post("/register/register")
                .params(validCustomerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/register/complete")); // リダイレクト先が "/register/complete" であることを確認

        // サービス呼び出しの検証
        verify(customerService, times(1)).registerCustomer(any(Customer.class));
    }

    @Test
    @DisplayName("POST /customers/register: 不正な入力で入力画面に戻る")
    void testRegisterCustomer_InvalidInput() throws Exception {
        // テストデータ（バリデーションエラーあり）
        MultiValueMap<String, String> invalidCustomerForm = new LinkedMultiValueMap<>();
        invalidCustomerForm.add("email", ""); // 空の値でバリデーションエラーを発生させる
        invalidCustomerForm.add("password", "short");
        invalidCustomerForm.add("confirmPassword", "mismatch");
        invalidCustomerForm.add("name", "");
        invalidCustomerForm.add("birthDate", "");
        invalidCustomerForm.add("phoneNumber", "");
        invalidCustomerForm.add("address", "");

        // テスト実行
        mockMvc.perform(post("/register/register")
                .params(invalidCustomerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")) // ビュー名が "customer-input" に戻ることを確認
                .andExpect(model().attributeHasFieldErrors("customerForm", "email", "password", "confirmPassword", "name", "birthDate", "phoneNumber", "address")); // バリデーションエラーを確認

        // サービス呼び出しが行われていないことを確認
        verify(customerService, times(0)).registerCustomer(any(Customer.class));
    }

    @Test
    @DisplayName("POST /customers/register: ビジネスエラーの場合エラー画面を表示する")
    void testHandleBusinessError() throws Exception {
        // モックの設定: CustomerService が IllegalArgumentException をスロー
        doThrow(new IllegalArgumentException("ビジネスエラーが発生しました"))
                .when(customerService).registerCustomer(any());

        // テストデータ
        MultiValueMap<String, String> validCustomerForm = new LinkedMultiValueMap<>();
        validCustomerForm.add("email", "valid.email@example.com");
        validCustomerForm.add("password", "password123");
        validCustomerForm.add("confirmPassword", "password123");
        validCustomerForm.add("name", "Valid User");
        validCustomerForm.add("birthDate", "1990-05-20");
        validCustomerForm.add("phoneNumber", "123-456-7890");
        validCustomerForm.add("address", "123 Main St");

        // テスト実行
        mockMvc.perform(post("/register/register")
                .params(validCustomerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-registration-error")) // ビュー名が "customer-registration-error" であることを確認
                .andExpect(model().attribute("errorMessage", "ビジネスエラーが発生しました")); // モデルにエラーメッセージが含まれていることを確認
    }

    @Test
    @DisplayName("GET /register/complete: 登録完了画面を表示する")
    void testShowCompletePage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/register/complete")) // GET リクエストを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-complete")); // ビュー名が "customer-complete" であることを確認
    }
}
