package io.github.yoshikawaa.example.ai_sample.config;

import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final LoginAttemptService loginAttemptService;
    private final LoginHistoryService loginHistoryService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * H2コンソール専用: SAMEORIGIN
     */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/h2-console/**")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            );
        return http.build();
    }

    /**
     * 通常用: X-Frame-Options DENY
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 管理者専用画面・API
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 認証不要画面
                .requestMatchers(
                    "/", "/register/**", "/login", "/password-reset/**", "/account-locked", "/account-unlock/**", "/session-limit-exceeded", "/error"
                ).permitAll()
                // その他は認証のみ必要
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "img-src 'self'; " +
                        "font-src 'self' https://cdn.jsdelivr.net; " +
                        "object-src 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self';"
                    )
                )
            )
            .csrf(withDefaults())
            .formLogin(form -> form
                .loginPage("/login") // カスタムログイン画面
                .successHandler(authenticationSuccessHandler()) // ログイン成功ハンドラー
                .failureHandler(authenticationFailureHandler()) // ログイン失敗ハンドラー
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler()) // ログアウト成功ハンドラー
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
                .sessionRegistry(sessionRegistry())
            );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String email = authentication.getName();
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            
            log.info("ログイン成功: email={}", email);
            
            // ログイン成功時は試行回数をリセット
            loginAttemptService.resetAttempts(email);
            
            // ログイン履歴を記録（セッション制御チェック後に実行される）
            loginHistoryService.recordLoginSuccess(email, ipAddress, userAgent);
            
            response.sendRedirect("/mypage");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String email = request.getParameter("username");
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            
            if (exception instanceof SessionAuthenticationException) {
                // 最大セッション数超過時
                log.warn("最大セッション数超過によるログイン拒否: email={}", email);
                loginHistoryService.recordSessionExceeded(email, ipAddress, userAgent);
                response.sendRedirect("/session-limit-exceeded");
                return;
            }
            if (exception instanceof LockedException) {
                // 6回目以降のロック中ログイン試行
                log.warn("ロック中のアカウントへのログイン試行: email={}, reason={}", email, exception.getMessage());
                loginHistoryService.recordLoginLocked(email, ipAddress, userAgent);
                response.sendRedirect("/account-locked?email=" + email);
                return;
            }
            // ログイン失敗を記録
            loginHistoryService.recordLoginFailure(email, ipAddress, userAgent, exception.getMessage());
            boolean locked = loginAttemptService.handleFailedLoginAttempt(email);
            if (locked) {
                // 5回目失敗で即ロック画面遷移
                log.warn("5回目のログイン失敗によるアカウントロック: email={}, reason={}", email, exception.getMessage());
                response.sendRedirect("/account-locked?email=" + email);
                return;
            }
            // 通常のログイン失敗（パスワード誤り等）
            log.warn("ログイン失敗: email={}, reason={}", email, exception.getMessage());
            response.sendRedirect("/login?error");
        };
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            String email = authentication.getName();
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            
            log.info("ログアウト: email={}", email);
            
            // ログアウト履歴を記録
            loginHistoryService.recordLogout(email, ipAddress, userAgent);
            
            response.sendRedirect("/");
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
