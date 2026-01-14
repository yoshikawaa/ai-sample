package io.github.yoshikawaa.example.ai_sample.config;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder; // PasswordEncoder を注入

    @MockitoBean
    private CustomerRepository customerRepository;

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
    void testLoginSuccess() throws Exception {
        // ログイン成功のテスト
        mockMvc.perform(formLogin("/login").user("test@example.com").password("password123")) // 正しい資格情報でログイン
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/mypage")); // ログイン成功後のリダイレクト先を確認
    }

    @Test
    void testLoginFailure() throws Exception {
        // ログイン失敗のテスト
        mockMvc.perform(formLogin("/login").user("invalid@example.com").password("wrongPassword")) // 無効な資格情報でログイン
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/login?error")); // ログイン失敗後のリダイレクト先を確認
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testAccessProtectedResource() throws Exception {
        // 認証済みユーザーが保護されたリソースにアクセスできることを確認
        mockMvc.perform(get("/mypage"))
                .andExpect(status().isOk()); // HTTP ステータスが 200 OK であることを確認
    }

    @Test
    @WithAnonymousUser // 未認証ユーザーを明示的にシミュレート
    void testAccessProtectedResourceWithoutAuthentication() throws Exception {
        // 未認証ユーザーが保護されたリソースにアクセスできないことを確認
        mockMvc.perform(get("/mypage"))
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrlPattern("**/login")); // ログインページにリダイレクトされることを確認
    }

    @Test
    @WithMockUser // モックユーザーで認証済みの状態をシミュレート
    void testLogout() throws Exception {
        // ログアウトのテスト
        mockMvc.perform(logout())
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/")); // ログアウト後のリダイレクト先を確認
    }
}