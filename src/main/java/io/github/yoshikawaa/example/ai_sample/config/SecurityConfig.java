package io.github.yoshikawaa.example.ai_sample.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/customers", "/customers/**", "/register/**", "/login", "/password-reset/**", "/h2-console/**", "/error").permitAll() // ログイン不要の画面
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
            log.info("ログイン成功: email={}", authentication.getName());
            response.sendRedirect("/mypage");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            log.warn("ログイン失敗: email={}, reason={}", username, exception.getMessage());
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
