package io.github.yoshikawaa.example.ai_sample.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

@Configuration
@ConditionalOnProperty(name = "app.greenmail.enabled", havingValue = "true", matchIfMissing = true)
public class GreenMailConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public GreenMail greenMail() {
        ServerSetup serverSetup = new ServerSetup(3025, "localhost", "smtp");
        serverSetup.setServerStartupTimeout(10000); // タイムアウトを10秒に延長
        serverSetup.setVerbose(false); // 詳細ログを無効化
        GreenMail greenMail = new GreenMail(serverSetup);
        greenMail.setUser("test@example.com", "password"); // ユーザーを設定（必要に応じて）
        return greenMail;
    }
}
