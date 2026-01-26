package io.github.yoshikawaa.example.ai_sample.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "app.security.login.attempt")
public class LoginAttemptProperties {
    /** 最大試行回数 */
    private int max = 5;
    /** ロック時間（ミリ秒） */
    private long lockDurationMs = 30 * 60 * 1000L;
}
