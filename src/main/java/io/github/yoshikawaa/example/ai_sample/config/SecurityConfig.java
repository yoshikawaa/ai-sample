package io.github.yoshikawaa.example.ai_sample.config;

import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Slf4j
@Configuration
public class SecurityConfig {

    private final LoginAttemptService loginAttemptService;

    public SecurityConfig(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/customers", "/customers/**", "/register/**", "/login", "/password-reset/**", "/account-locked", "/h2-console/**", "/error").permitAll() // ログイン不要の画面
                .anyRequest().authenticated() // それ以外は認証が必要
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()) // H2コンソール用にiframeを許可
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**") // H2コンソール用にCSRFを無効化
            )
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
            if (exception instanceof LockedException) {
                // ロック中のログイン試行
                log.warn("ロック中のアカウントへのログイン試行: email={}", email);
                response.sendRedirect("/account-locked?email=" + email);
                return;
            }
            boolean locked = loginAttemptService.handleFailedLoginAttempt(email);
            if (locked) {
                // 5回目失敗時のWARNログ（通常失敗ログはこの場合は出力しない）
                log.warn("5回目失敗で即ロック画面遷移: email={}, reason={}", email, exception.getMessage());
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
}
