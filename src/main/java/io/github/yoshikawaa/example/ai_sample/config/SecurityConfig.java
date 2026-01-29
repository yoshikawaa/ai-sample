package io.github.yoshikawaa.example.ai_sample.config;

import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
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
                .requestMatchers(
                    "/", "/customers", "/customers/**", "/register/**", "/login", "/password-reset/**", "/account-locked",
                    "/account-unlock/**", "/error"
                ).permitAll() // ログイン不要の画面（※/h2-console/**は除外）
                .anyRequest().authenticated() // それ以外は認証が必要
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "style-src 'self' https://cdn.jsdelivr.net; " +
                        "script-src 'self'; " +
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
            log.info("ログイン成功: email={}", email);
            // ログイン成功時は試行回数をリセット
            loginAttemptService.resetAttempts(email);
            response.sendRedirect("/mypage");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String email = request.getParameter("username");
            // 最大セッション数超過時
            if (exception instanceof SessionAuthenticationException) {
                log.warn("最大セッション数超過によるログイン拒否: email={}", email);
                response.sendRedirect("/session-limit-exceeded");
                return;
            }
            if (exception instanceof LockedException) {
                // 6回目以降のロック中ログイン試行
                log.warn("ロック中のアカウントへのログイン試行: email={}, reason={}", email, exception.getMessage());
                response.sendRedirect("/account-locked?email=" + email);
                return;
            }
            boolean locked = loginAttemptService.handleFailedLoginAttempt(email);
            if (locked) {
                // 5回目失敗で即ロック画面遷移: email={}, reason={}", email, exception.getMessage());
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
            log.info("ログアウト: email={}", authentication.getName());
            response.sendRedirect("/");
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
