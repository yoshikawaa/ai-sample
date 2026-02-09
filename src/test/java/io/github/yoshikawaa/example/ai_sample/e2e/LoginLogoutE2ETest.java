package io.github.yoshikawaa.example.ai_sample.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * E2E: ログイン・ログアウト機能のテスト
 *
 * 注意: 各テストメソッドは異なるユーザーを使用してテストメソッド間の独立性を確保
 */
@DisplayName("E2E: ログイン・ログアウト機能")
class LoginLogoutE2ETest extends E2ETestBase {

    @Test
    @DisplayName("正常系: ログインに成功し、マイページが表示される")
    void testSuccessfulLogin() {
        // 1. ログイン画面へ遷移
        open("/login");
        $("#page-title").shouldHave(text("Login"));

        // 2. ログイン（john.doeを使用）
        login("john.doe@example.com", "password");

        // 3. マイページへリダイレクトされる
        $("#page-title").shouldHave(text("My Page"));
        $("#user-email").shouldHave(text("john.doe@example.com"));
    }

    @Test
    @DisplayName("異常系: パスワードが間違っている場合、エラーメッセージが表示される")
    void testLoginWithWrongPassword() {
        // 1. ログイン画面へ遷移
        open("/login");
        $("#page-title").shouldHave(text("Login"));

        // 2. 間違ったパスワードでログイン試行（jane.doeを使用）
        login("jane.doe@example.com", "wrongpassword");

        // 3. ログイン画面に留まり、エラーメッセージが表示される
        $("#page-title").shouldHave(text("Login"));
        $("#error-message").shouldBe(visible);
    }

    @Test
    @DisplayName("異常系: 存在しないユーザーでログインしようとすると、エラーメッセージが表示される")
    void testLoginWithNonExistentUser() {
        // 1. ログイン画面へ遷移
        open("/login");
        $("#page-title").shouldHave(text("Login"));

        // 2. 存在しないメールアドレスでログイン試行
        login("nonexistent@example.com", "password");

        // 3. ログイン画面に留まり、エラーメッセージが表示される
        $("#page-title").shouldHave(text("Login"));
        $("#error-message").shouldBe(visible);
    }

    @Test
    @DisplayName("正常系: ログアウトに成功し、ホーム画面へリダイレクトされる")
    void testSuccessfulLogout() {
        // 1. ログイン（alice.smithを使用）
        login("alice.smith@example.com", "password");
        $("#page-title").shouldHave(text("My Page"));

        // 2. マイページからログアウトボタンをクリック
        open("/mypage");
        $("form[action='/logout'] button[type=submit]").click();

        // 3. ホーム画面へリダイレクトされる
        $("#page-title").shouldHave(text("Welcome to the Spring Boot Application!"));
    }

    @Test
    @DisplayName("異常系: 5回連続でログイン失敗するとアカウントがロックされる")
    void testAccountLockAfterMultipleFailedAttempts() {
        // 1. 4回連続でログイン失敗（bob.johnsonを使用）
        for (int i = 0; i < 4; i++) {
            open("/login");
            login("bob.johnson@example.com", "wrongpassword");
            $("#error-message").shouldBe(visible);  // 4回目まではエラーメッセージ
        }

        // 2. 5回目のログイン失敗でアカウントロック
        open("/login");
        login("bob.johnson@example.com", "wrongpassword");
        $("#page-title").shouldHave(text("Account Locked"));
        $("#lock-message").shouldBe(visible);
    }
}
