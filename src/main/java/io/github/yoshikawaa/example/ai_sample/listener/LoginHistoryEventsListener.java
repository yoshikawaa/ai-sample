package io.github.yoshikawaa.example.ai_sample.listener;

import org.springframework.security.authentication.event.LogoutSuccessEvent;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import io.github.yoshikawaa.example.ai_sample.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * ログイン履歴に関する複数のイベントを監視するリスナ
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LoginHistoryEventsListener {

    private final LoginHistoryService loginHistoryService;

    /**
     * ログイン成功イベントを処理
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        String ipAddress = RequestContextUtil.getClientIpAddress();
        String userAgent = RequestContextUtil.getUserAgent();

        log.debug("AuthenticationSuccessEvent を受信: email={}, ip={}", email, ipAddress);
        loginHistoryService.recordLoginSuccess(email, ipAddress, userAgent);
    }

    /**
     * ログイン失敗イベントを処理
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        String ipAddress = RequestContextUtil.getClientIpAddress();
        String userAgent = RequestContextUtil.getUserAgent();
        String exceptionMessage = event.getException().getMessage();

        log.debug("AbstractAuthenticationFailureEvent を受信: email={}, ip={}, reason={}", 
            email, ipAddress, exceptionMessage);
        loginHistoryService.recordLoginFailure(email, ipAddress, userAgent, exceptionMessage);
    }

    /**
     * ログアウト成功イベントを処理
     */
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        String email = event.getAuthentication().getName();
        String ipAddress = RequestContextUtil.getClientIpAddress();
        String userAgent = RequestContextUtil.getUserAgent();

        log.debug("LogoutSuccessEvent を受信: email={}, ip={}", email, ipAddress);
        loginHistoryService.recordLogout(email, ipAddress, userAgent);
    }
}
