package io.github.yoshikawaa.example.ai_sample.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

/**
 * E2E: 管理者ダッシュボード機能のテスト
 *
 * 注意: 各テストメソッドは異なるユーザーを使用してテストメソッド間の独立性を確保
 */
@DisplayName("E2E: 管理者ダッシュボード機能")
public class AdminDashboardE2ETest extends E2ETestBase {

    @AfterEach
    void tearDown() {
        // テスト後にログアウトしてセッションをクリア
        open("/mypage");
        $$("button[type=submit]").findBy(text("Logout")).click();
    }

    @Test
    @DisplayName("正常系: 管理者ダッシュボードが表示される")
    void testShowAdminDashboard() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 管理者ダッシュボードへ遷移
        open("/admin/dashboard");
        $("#page-title").shouldHave(text("Admin Dashboard"));

        // 3. ダッシュボードの主要要素が表示される
        $("a[href='/admin/customers']").shouldBe(visible);
        $("a[href='/admin/statistics']").shouldBe(visible);
        $("a[href='/admin/login-history']").shouldBe(visible);
        $("a[href='/admin/audit-log']").shouldBe(visible);
        $("a[href='/admin/notification-history']").shouldBe(visible);
    }
}
