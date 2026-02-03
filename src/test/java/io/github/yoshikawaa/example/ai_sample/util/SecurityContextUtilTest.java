package io.github.yoshikawaa.example.ai_sample.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityContextUtil のテスト")
class SecurityContextUtilTest {

    @AfterEach
    void tearDown() {
        // 各テスト後にSecurityContextをクリア
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("認証済みの場合、認証ユーザー名を返す")
    void testGetAuthenticatedUsername_WithAuthentication() {
        // 認証情報をセット
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken("admin@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // メソッドを呼び出し
        String result = SecurityContextUtil.getAuthenticatedUsername("fallback@example.com");

        // 検証: 認証ユーザー名が返される
        assertThat(result).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("未認証の場合、フォールバック値を返す")
    void testGetAuthenticatedUsername_WithoutAuthentication() {
        // SecurityContextをクリア（未認証状態）
        SecurityContextHolder.clearContext();

        // メソッドを呼び出し
        String result = SecurityContextUtil.getAuthenticatedUsername("fallback@example.com");

        // 検証: フォールバック値が返される
        assertThat(result).isEqualTo("fallback@example.com");
    }

    @Test
    @DisplayName("認証情報がnullの場合、フォールバック値を返す")
    void testGetAuthenticatedUsername_NullAuthentication() {
        // 明示的にnullをセット
        SecurityContextHolder.getContext().setAuthentication(null);

        // メソッドを呼び出し
        String result = SecurityContextUtil.getAuthenticatedUsername("fallback@example.com");

        // 検証: フォールバック値が返される
        assertThat(result).isEqualTo("fallback@example.com");
    }
}
