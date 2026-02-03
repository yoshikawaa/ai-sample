package io.github.yoshikawaa.example.ai_sample.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext関連のユーティリティクラス
 */
public class SecurityContextUtil {

    private SecurityContextUtil() {
        // ユーティリティクラスのためインスタンス化を禁止
    }

    /**
     * 現在の認証ユーザー名を取得。未認証の場合はフォールバック値を返す
     * 
     * @param fallbackValue 未認証時に返すフォールバック値
     * @return 認証済みの場合は認証ユーザー名、未認証の場合はfallbackValue
     */
    public static String getAuthenticatedUsername(String fallbackValue) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : fallbackValue;
    }
}
