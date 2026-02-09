package io.github.yoshikawaa.example.ai_sample.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.SetValueOptions.withDate;

/**
 * E2E: マイページ機能のテスト
 *
 * 注意: 各テストメソッドは異なるユーザーを使用してテストメソッド間の独立性を確保
 */
@DisplayName("E2E: マイページ機能")
class MyPageE2ETest extends E2ETestBase {

    @Test
    @DisplayName("正常系: マイページが表示され、顧客情報が確認できる")
    void testShowMyPage() {
        // 1. ログイン（john.doeを使用）
        login("john.doe@example.com", "password");

        // 2. マイページへ遷移
        open("/mypage");
        $("#page-title").shouldHave(text("My Page"));

        // 3. 顧客情報が表示される
        $("#user-name").shouldHave(text("John Doe"));
        $("#user-email").shouldHave(text("john.doe@example.com"));
    }

    @Test
    @DisplayName("正常系: 顧客情報を編集できる（入力→確認→完了）")
    void testEditCustomerInfo() {
        // 1. ログイン（jane.doeを使用）
        login("jane.doe@example.com", "password");

        // 2. マイページから編集画面へ遷移
        open("/mypage");
        $("a[href='/mypage/edit']").click();
        $("#page-title").shouldHave(text("Edit Customer Information"));

        // 3. 顧客情報を入力
        $("#name").setValue("Jane Doe Updated");
        $("#birthDate").setValue(withDate(LocalDate.of(1985, 5, 15)));
        $("#phoneNumber").setValue("555-9999");
        $("#address").setValue("456 Updated St");

        // 4. 確認画面へ遷移
        $("button[type=submit]").click();
        $("#page-title").shouldHave(text("Confirm Update"));

        // 5. 確認画面で入力内容を検証
        $("#confirm-name").shouldHave(text("Jane Doe Updated"));
        $("#confirm-birthDate").shouldHave(text("1985-05-15"));
        $("#confirm-phoneNumber").shouldHave(text("555-9999"));
        $("#confirm-address").shouldHave(text("456 Updated St"));

        // 6. 更新実行
        $$("button[type=submit]").findBy(text("Update")).click();

        // 7. 完了画面へ遷移
        $("#page-title").shouldHave(text("Update Complete"));

        // 8. マイページへ戻り、更新内容を確認
        $("a[href='/mypage']").click();
        $("#user-name").shouldHave(text("Jane Doe Updated"));
    }

    @Test
    @DisplayName("正常系: パスワードを変更できる")
    void testChangePassword() {
        // 1. ログイン（alice.smithを使用）
        login("alice.smith@example.com", "password");

        // 2. マイページからパスワード変更画面へ遷移
        open("/mypage");
        $("a[href='/mypage/change-password']").click();
        $("#page-title").shouldHave(text("Change Password"));

        // 3. パスワード変更情報を入力
        $("#currentPassword").setValue("password");
        $("#newPassword").setValue("newpassword123");
        $("#confirmPassword").setValue("newpassword123");

        // 4. パスワード変更実行
        $("button[type=submit]").click();

        // 5. 完了画面へ遷移
        $("#page-title").shouldHave(text("Password Change Complete"));

        // 6. マイページへ戻る
        $("a[href='/mypage']").click();
        $("#page-title").shouldHave(text("My Page"));
    }

    @Test
    @DisplayName("正常系: アカウントを削除できる（確認→完了）")
    void testDeleteCustomer() {
        // 1. ログイン（bob.johnsonを使用）
        login("bob.johnson@example.com", "password");

        // 2. マイページから削除画面へ遷移
        open("/mypage");
        $("a[href='/mypage/delete']").click();
        $("#page-title").shouldHave(text("Account Deletion"));

        // 3. 確認画面へ遷移
        $("button[type=submit]").click();
        $("#page-title").shouldHave(text("Account Deletion Confirmation"));

        // 4. 削除実行
        $$("button[type=submit]").findBy(text("Delete")).click();

        // 5. 完了画面へ遷移（Account Deleted画面のみを検証）
        $("#page-title").shouldHave(text("Account Deleted"));
    }

    @Test
    @DisplayName("異常系: 編集時にバリデーションエラーが表示される")
    void testEditCustomerInfoValidationError() {
        // 1. ログイン（carol.whiteを使用）
        login("carol.white@example.com", "password");

        // 2. マイページから編集画面へ遷移
        open("/mypage");
        $("a[href='/mypage/edit']").click();
        $("#page-title").shouldHave(text("Edit Customer Information"));

        // 3. 無効な情報を入力（名前を空にする）
        $("#name").clear();
        $("#birthDate").setValue(withDate(LocalDate.of(1985, 5, 15)));
        $("#phoneNumber").setValue("555-9999");
        $("#address").setValue("456 Updated St");

        // 4. 送信
        $("button[type=submit]").click();

        // 5. エラーメッセージが表示される
        $("#page-title").shouldHave(text("Edit Customer Information"));  // 入力画面に留まる
        $("#name-error").shouldBe(visible);  // バリデーションエラー
    }

    @Test
    @DisplayName("異常系: パスワード変更時に現在のパスワードが間違っている")
    void testChangePasswordWrongCurrentPassword() {
        // 1. ログイン（david.brownを使用）
        login("david.brown@example.com", "password");

        // 2. マイページからパスワード変更画面へ遷移
        open("/mypage");
        $("a[href='/mypage/change-password']").click();
        $("#page-title").shouldHave(text("Change Password"));

        // 3. 間違った現在のパスワードを入力
        $("#currentPassword").setValue("wrongpassword");
        $("#newPassword").setValue("newpassword123");
        $("#confirmPassword").setValue("newpassword123");

        // 4. パスワード変更実行
        $("button[type=submit]").click();

        // 5. エラーメッセージが表示される
        $("#page-title").shouldHave(text("Change Password"));  // 入力画面に留まる
        $("#currentPassword-error").shouldBe(visible);
    }
}
