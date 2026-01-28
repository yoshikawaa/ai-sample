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
import org.junit.jupiter.api.Disabled;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;

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
    @Disabled("spring-projects/spring-security#4212 Spring Securityの不具合により多重ログイン制御が動作しないため、一時的に無効化。")
    @DisplayName("多重ログイン制御: 1回目のセッションがSessionRegistryに登録されている状態で、2回目のログインは最大セッション数超過エラー画面に遷移する")
    void testSessionSurvivesAfterSecondLoginAttempt() throws Exception {
        // 1回目のログイン（セッションA）
        MvcResult resultA = mockMvc.perform(post("/login")
                .param("username", "test@example.com")
                .param("password", "password123")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        MockHttpSession sessionA = (MockHttpSession) resultA.getRequest().getSession();

        // セッションAで認証済みページにアクセス（セッションAは有効なまま）
        mockMvc.perform(get("/mypage").session(sessionA))
            .andExpect(status().isOk());

        // 2回目のログイン（セッションB: 新しいセッション）
        MockHttpSession sessionB = new MockHttpSession();
        mockMvc.perform(post("/login")
            .param("username", "test@example.com")
            .param("password", "password123")
            .with(csrf())
            .session(sessionB))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/session-limit-exceeded"));
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
    @DisplayName("testLogout: ログアウトが成功しトップページにリダイレクトされる")
    @WithMockUser
    void testLogout() throws Exception {
        // 認証済みユーザーで /logout にアクセス
        mockMvc.perform(logout())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("CSPヘッダが付与される")
    void testContentSecurityPolicyHeader() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Security-Policy",
                containsString("default-src 'self'")));
    }

        @Test
        @DisplayName("X-Frame-OptionsヘッダがDENYで付与される")
        void testXFrameOptionsHeader() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        @Test
        @DisplayName("CSRFトークン未付与時は403 Forbiddenになる")
        void testCsrfTokenRequired() throws Exception {
            mockMvc.perform(post("/login")
                    .param("username", "test@example.com")
                    .param("password", "password123"))
                .andExpect(status().isForbidden());
        }
}