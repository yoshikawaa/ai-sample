package io.github.yoshikawaa.example.ai_sample.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security.account-unlock")
public class AccountUnlockProperties {
    /** トークン有効期限（秒） */
    private int tokenExpirySeconds = 3600;
    /** アプリケーションのホスト＋ポート（例: http://localhost:8080） */
    private String hostUrl = "http://localhost:8080";
}
