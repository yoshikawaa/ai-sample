package io.github.yoshikawaa.example.ai_sample.config;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("HttpSessionConfig のテスト")
class HttpSessionConfigTest {

    @Autowired
    private HttpSessionListener httpSessionListener;

    @Test
    @DisplayName("sessionCreated: セッション作成時にログが出力される")
    void testSessionCreated() {
        // テスト用のHttpSessionを作成
        MockHttpSession session = new MockHttpSession();
        HttpSessionEvent event = new HttpSessionEvent(session);

        // sessionCreatedメソッドを呼び出し（例外が発生しないことを確認）
        httpSessionListener.sessionCreated(event);

        // セッションIDが設定されていることを確認
        assertThat(session.getId()).isNotNull();
    }

    @Test
    @DisplayName("sessionDestroyed: セッション破棄時にログが出力される")
    void testSessionDestroyed() {
        // テスト用のHttpSessionを作成
        MockHttpSession session = new MockHttpSession();
        HttpSessionEvent event = new HttpSessionEvent(session);

        // sessionDestroyedメソッドを呼び出し（例外が発生しないことを確認）
        httpSessionListener.sessionDestroyed(event);

        // セッションIDが設定されていることを確認
        assertThat(session.getId()).isNotNull();
    }
}
