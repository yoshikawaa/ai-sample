package io.github.yoshikawaa.example.ai_sample.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .defaultSuccessUrl("/mypage", true) // ログイン成功後の遷移先
                .failureUrl("/login?error") // 認証失敗時のリダイレクト先
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/") // ログアウト後の遷移先
                .permitAll()
            );
        return http.build();
    }
}
