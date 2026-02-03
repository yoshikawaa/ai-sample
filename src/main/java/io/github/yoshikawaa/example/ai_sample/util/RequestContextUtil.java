package io.github.yoshikawaa.example.ai_sample.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HTTPリクエストコンテキストからの情報取得ユーティリティ
 */
public class RequestContextUtil {

    private RequestContextUtil() {
        // ユーティリティクラスのためインスタンス化を禁止
    }

    /**
     * 現在のHTTPリクエストからIPアドレスを取得
     * 
     * @return IPアドレス（取得できない場合は "unknown"）
     */
    public static String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // カンマ区切りの場合、最初のIPを取得
            int commaIndex = ip.indexOf(',');
            if (commaIndex != -1) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }
        
        ip = request.getHeader("Proxy-Client-IP");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    /**
     * RequestContextHolderから現在のHttpServletRequestを取得
     * 
     * @return HttpServletRequest（取得できない場合は null）
     */
    private static HttpServletRequest getCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
