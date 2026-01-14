package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
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
class CustomerRegistrationControllerTest {

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShowInputForm() throws Exception {
        // テスト実行
        mockMvc.perform(get("/customers/input"))
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")); // ビュー名が "customer-input" であることを確認
    }

    @Test
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
        mockMvc.perform(post("/customers/confirm")
                .params(validCustomerForm) // params を使用してフォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-confirm")) // ビュー名が "customer-confirm" であることを確認
                .andExpect(model().attributeHasNoErrors("customerForm")); // バリデーションエラーがないことを確認
        }

    @Test
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
        mockMvc.perform(post("/customers/confirm")
                .params(invalidCustomerForm) // params を使用してフォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")) // ビュー名が "customer-input" に戻ることを確認
                .andExpect(model().attributeHasFieldErrors("customerForm", "email", "name", "password", "confirmPassword", "birthDate", "phoneNumber", "address")); // バリデーションエラーを確認
    }

    @Test
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
        mockMvc.perform(post("/customers/input")
                .params(customerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")); // ビュー名が "customer-input" であることを確認
    }

    @Test
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
        mockMvc.perform(post("/customers/register")
                .params(validCustomerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/customers/complete")); // リダイレクト先が "/customers/complete" であることを確認

        // サービス呼び出しの検証
        verify(customerService, times(1)).registerCustomer(any(Customer.class));
    }

    @Test
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
        mockMvc.perform(post("/customers/register")
                .params(invalidCustomerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-input")) // ビュー名が "customer-input" に戻ることを確認
                .andExpect(model().attributeHasFieldErrors("customerForm", "email", "password", "confirmPassword", "name", "birthDate", "phoneNumber", "address")); // バリデーションエラーを確認

        // サービス呼び出しが行われていないことを確認
        verify(customerService, times(0)).registerCustomer(any(Customer.class));
    }

    @Test
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
        mockMvc.perform(post("/customers/register")
                .params(validCustomerForm) // フォームデータを送信
                .with(csrf())) // CSRF トークンを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("business-error")) // ビュー名が "business-error" であることを確認
                .andExpect(model().attribute("errorMessage", "ビジネスエラーが発生しました")); // モデルにエラーメッセージが含まれていることを確認
    }

    @Test
    void testShowCompletePage() throws Exception {
        // テスト実行
        mockMvc.perform(get("/customers/complete")) // GET リクエストを送信
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-complete")); // ビュー名が "customer-complete" であることを確認
    }
}
