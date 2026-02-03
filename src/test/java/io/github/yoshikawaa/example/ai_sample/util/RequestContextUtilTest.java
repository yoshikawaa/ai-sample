package io.github.yoshikawaa.example.ai_sample.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestContextUtil のテスト")
class RequestContextUtilTest {

    @AfterEach
    void tearDown() {
        // テスト後にRequestContextHolderをクリア
        RequestContextHolder.resetRequestAttributes();
    }

    // ========================================
    // クライアントIPアドレス取得
    // ========================================

    @Nested
    @DisplayName("getClientIpAddress: クライアントIPアドレス取得")
    class GetClientIpAddressTest {

        @Test
        @DisplayName("X-Forwarded-ForヘッダーからクライアントIPアドレスを取得できる")
        void testGetClientIpAddress_FromXForwardedFor() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "203.0.113.195");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("203.0.113.195");
        }

        @Test
        @DisplayName("X-Forwarded-Forヘッダーに複数のIPアドレスがある場合、最初のIPを取得できる")
        void testGetClientIpAddress_FromXForwardedForWithMultipleIps() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "203.0.113.195, 198.51.100.178, 192.0.2.1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証: カンマ区切りの最初のIPを取得
            assertThat(clientIp).isEqualTo("203.0.113.195");
        }

        @Test
        @DisplayName("X-Forwarded-Forヘッダーがない場合、RemoteAddrからIPアドレスを取得できる")
        void testGetClientIpAddress_FromRemoteAddr() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.100");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("X-Forwarded-Forヘッダーが空の場合、RemoteAddrからIPアドレスを取得できる")
        void testGetClientIpAddress_FromRemoteAddrWhenXForwardedForEmpty() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "");
            request.setRemoteAddr("192.168.1.100");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("X-Forwarded-Forがunknownの場合、Proxy-Client-IPを確認する")
        void testGetClientIpAddress_FromProxyClientIp() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", "203.0.113.50");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("Proxy-Client-IPがunknownの場合、WL-Proxy-Client-IPを確認する")
        void testGetClientIpAddress_FromWLProxyClientIp() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", "unknown");
            request.addHeader("WL-Proxy-Client-IP", "203.0.113.75");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("203.0.113.75");
        }

        @Test
        @DisplayName("Proxy-Client-IPが空の場合、WL-Proxy-Client-IPを確認する")
        void testGetClientIpAddress_FromWLProxyClientIpWhenProxyClientIpEmpty() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", "");
            request.addHeader("WL-Proxy-Client-IP", "203.0.113.80");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("203.0.113.80");
        }

        @Test
        @DisplayName("WL-Proxy-Client-IPがunknownの場合、RemoteAddrを確認する")
        void testGetClientIpAddress_FromRemoteAddrWhenWLProxyClientIpUnknown() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", "unknown");
            request.addHeader("WL-Proxy-Client-IP", "unknown");
            request.setRemoteAddr("192.168.1.200");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("192.168.1.200");
        }

        @Test
        @DisplayName("WL-Proxy-Client-IPが空の場合、RemoteAddrを確認する")
        void testGetClientIpAddress_FromRemoteAddrWhenWLProxyClientIpEmpty() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", "unknown");
            request.addHeader("WL-Proxy-Client-IP", "");
            request.setRemoteAddr("192.168.1.201");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("192.168.1.201");
        }

        @Test
        @DisplayName("すべてのヘッダーとRemoteAddrが空の場合、unknown を返す")
        void testGetClientIpAddress_AllHeadersAndRemoteAddrEmpty() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "");
            request.addHeader("Proxy-Client-IP", "");
            request.addHeader("WL-Proxy-Client-IP", "");
            request.setRemoteAddr("");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("unknown");
        }

        @Test
        @DisplayName("RequestContextHolderがnullの場合、unknown を返す")
        void testGetClientIpAddress_WhenRequestContextHolderIsNull() {
            // RequestContextHolderをクリア
            RequestContextHolder.resetRequestAttributes();

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("unknown");
        }

        @Test
        @DisplayName("リクエストがnullの場合、unknown を返す")
        void testGetClientIpAddress_WhenRequestIsNull() {
            // RequestContextHolder に null 相当の状態を設定
            // （ServletRequestAttributes が null の HttpServletRequest を持つ）
            RequestContextHolder.resetRequestAttributes();

            // メソッドを実行
            String clientIp = RequestContextUtil.getClientIpAddress();

            // 検証
            assertThat(clientIp).isEqualTo("unknown");
        }
    }

    // ========================================
    // User-Agent取得
    // ========================================

    @Nested
    @DisplayName("getUserAgent: User-Agent取得")
    class GetUserAgentTest {

        @Test
        @DisplayName("User-Agentヘッダーから値を取得できる")
        void testGetUserAgent() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String userAgent = RequestContextUtil.getUserAgent();

            // 検証
            assertThat(userAgent).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        }

        @Test
        @DisplayName("User-Agentヘッダーがない場合、unknown を返す")
        void testGetUserAgent_WhenHeaderIsNull() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String userAgent = RequestContextUtil.getUserAgent();

            // 検証
            assertThat(userAgent).isEqualTo("unknown");
        }

        @Test
        @DisplayName("User-Agentヘッダーが空の場合、unknown を返す")
        void testGetUserAgent_WhenHeaderIsEmpty() {
            // テストデータ
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("User-Agent", "");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            // メソッドを実行
            String userAgent = RequestContextUtil.getUserAgent();

            // 検証
            assertThat(userAgent).isEqualTo("unknown");
        }

        @Test
        @DisplayName("RequestContextHolderがnullの場合、unknown を返す")
        void testGetUserAgent_WhenRequestContextHolderIsNull() {
            // RequestContextHolderをクリア
            RequestContextHolder.resetRequestAttributes();

            // メソッドを実行
            String userAgent = RequestContextUtil.getUserAgent();

            // 検証
            assertThat(userAgent).isEqualTo("unknown");
        }
    }
}
