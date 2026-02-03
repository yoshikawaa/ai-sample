package io.github.yoshikawaa.example.ai_sample.listener;

import org.springframework.security.authentication.event.LogoutSuccessEvent;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("LoginHistoryEventsListener のテスト")
class LoginHistoryEventsListenerTest {

    @Autowired
    private LoginHistoryEventsListener loginHistoryEventsListener;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // モックリクエストを設定
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("192.168.1.1");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @Test
    @DisplayName("ログイン成功イベントを処理して履歴を記録する")
    void testOnAuthenticationSuccess() {
        // given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "test@example.com",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);

        // when
        loginHistoryEventsListener.onAuthenticationSuccess(event);

        // then
        verify(loginHistoryService).recordLoginSuccess(
            eq("test@example.com"),
            eq("192.168.1.1"),
            eq("Mozilla/5.0")
        );
    }

    @Test
    @DisplayName("ログアウト成功イベントを処理して履歴を記録する")
    void testOnLogoutSuccess() {
        // given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "test@example.com",
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        LogoutSuccessEvent event = new LogoutSuccessEvent(authentication);

        // when
        loginHistoryEventsListener.onLogoutSuccess(event);

        // then
        verify(loginHistoryService).recordLogout(
            eq("test@example.com"),
            eq("192.168.1.1"),
            eq("Mozilla/5.0")
        );
    }
}
