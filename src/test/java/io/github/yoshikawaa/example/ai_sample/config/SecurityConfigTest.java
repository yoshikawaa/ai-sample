package io.github.yoshikawaa.example.ai_sample.config;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.LoginAttempt;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.repository.LoginAttemptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("SecurityConfig のテスト")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder; // PasswordEncoder を注入

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private LoginAttemptRepository loginAttemptRepository;

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

    // ...existing code...

    @Test
    @DisplayName("testLoginSuccess: ログインが成功する")
    void testLoginSuccess() throws Exception {
        // ログイン成功のテスト
        mockMvc.perform(formLogin("/login").user("test@example.com").password("password123")) // 正しい資格情報でログイン
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/mypage")); // ログイン成功後のリダイレクト先を確認
    }

    @Test
    @DisplayName("testLoginFailure: ログインが失敗する")
    void testLoginFailure() throws Exception {
        // ログイン失敗のテスト
        mockMvc.perform(formLogin("/login").user("invalid@example.com").password("wrongPassword")) // 無効な資格情報でログイン
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/login?error")); // ログイン失敗後のリダイレクト先を確認
    }

    @Test
    @DisplayName("ロック状態のユーザーは/account-lockedにリダイレクトされる（認証フロー全体）")
    void testLogin_LockedUser_RedirectsToAccountLocked() throws Exception {
        // ロック状態のLoginAttemptを返す
        String email = "locked@example.com";
        LoginAttempt lockedAttempt = new LoginAttempt();
        lockedAttempt.setEmail(email);
        lockedAttempt.setAttemptCount(5);
        lockedAttempt.setLastAttemptTime(System.currentTimeMillis());
        // 未来時刻でロック中
        lockedAttempt.setLockedUntil(System.currentTimeMillis() + 1000000L);
        when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(lockedAttempt));

        // ログイン試行→ロック画面にリダイレクトされることを検証
        mockMvc.perform(formLogin("/login").user(email).password("password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account-locked?email=" + email));
    }

    @Test
    @DisplayName("5回目失敗で即ロック画面に遷移する（認証フロー全体）")
    void testLogin_ImmediateLock_RedirectsToAccountLocked() throws Exception {
        // 5回目失敗時にロック状態のLoginAttemptを返す
        String email = "test@example.com";
        LoginAttempt lockedAttempt = new LoginAttempt();
        lockedAttempt.setEmail(email);
        lockedAttempt.setAttemptCount(5);
        lockedAttempt.setLastAttemptTime(System.currentTimeMillis());
        lockedAttempt.setLockedUntil(System.currentTimeMillis() + 1000000L);
        when(loginAttemptRepository.findByEmail(email)).thenReturn(Optional.of(lockedAttempt));

        // ログイン試行→ロック画面にリダイレクトされることを検証
        mockMvc.perform(formLogin("/login").user(email).password("password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account-locked?email=" + email));
    }



    @Test
    @DisplayName("testAccessProtectedResource: 認証済みユーザーは保護されたリソースにアクセスできる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testAccessProtectedResource() throws Exception {
        // 認証済みユーザーが保護されたリソースにアクセスできることを確認
        mockMvc.perform(get("/mypage"))
                .andExpect(status().isOk()); // HTTP ステータスが 200 OK であることを確認
    }

    @Test
    @DisplayName("testAccessProtectedResourceWithoutAuthentication: 未認証ユーザーはログインページにリダイレクトされる")
    @WithAnonymousUser // 未認証ユーザーを明示的にシミュレート
    void testAccessProtectedResourceWithoutAuthentication() throws Exception {
        // 未認証ユーザーが保護されたリソースにアクセスできないことを確認
        mockMvc.perform(get("/mypage"))
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrlPattern("**/login")); // ログインページにリダイレクトされることを確認
    }

    @Test
    @DisplayName("testLogout: ログアウトが成功する")
    @WithMockUser // モックユーザーで認証済みの状態をシミュレート
    void testLogout() throws Exception {
        // ログアウトのテスト
        mockMvc.perform(logout())
                .andExpect(status().is3xxRedirection()) // リダイレクトが発生することを確認
                .andExpect(redirectedUrl("/")); // ログアウト後のリダイレクト先を確認
    }
}