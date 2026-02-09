package io.github.yoshikawaa.example.ai_sample.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.yoshikawaa.example.ai_sample.config.LoginAttemptProperties;
import io.github.yoshikawaa.example.ai_sample.model.Customer;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

/**
 * E2E: 管理者顧客機能のテスト
 *
 * 注意: 各テストメソッドは異なるユーザーを使用してテストメソッド間の独立性を確保
 */
@DisplayName("E2E: 管理者顧客機能")
public class AdminCustomerE2ETest extends E2ETestBase {

    @Autowired
    private LoginAttemptProperties loginAttemptProperties;

    @AfterEach
    void tearDown() {
        // テスト後にログアウトしてセッションをクリア
        open("/mypage");
        $$("button[type=submit]").findBy(text("Logout")).click();
    }

    @Test
    @DisplayName("正常系: 管理者顧客一覧が表示される")
    void testShowAdminCustomerList() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 管理者顧客一覧へ遷移
        open("/admin/customers");
        $("#page-title").shouldHave(text("Customer List"));

        // 3. 顧客一覧の主要要素が表示される
        $("#customer-table").shouldBe(visible);
        $$("table#customer-table tbody tr").findBy(text("maria.harris@example.com")).shouldBe(visible);

        // 4. ページネーションできることを確認
        $("#pagination").shouldBe(visible);
        $("#pagination").$$("a").findBy(text("Next »")).click();
        $("#customer-table").shouldBe(visible);
        $$("table#customer-table tbody tr").findBy(text("john.doe@example.com")).shouldBe(visible);

        // 5. 検索機能が動作することを確認
        $("#name").setValue("admin");
        $("button[type=submit]").click();
        $("#customer-table").shouldBe(visible);
        $$("table#customer-table tbody tr").findBy(text("admin@example.com")).shouldBe(visible);
    }

    @Test
    @DisplayName("正常系: 管理者顧客登録ができる（入力→確認→完了）")
    void testRegisterAdminCustomer() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 管理者顧客登録画面へ遷移
        open("/admin/customers/registration-input");
        $("#page-title").shouldHave(text("Customer Registration"));

        // 3. フォーム入力
        $("#email").setValue("newuser@example.com");
        $("#password").setValue("Password123!");
        $("#confirmPassword").setValue("Password123!");
        $("#name").setValue("New User");
        $("#birthDate").setValue(withDate(LocalDate.of(1990, 1, 1)));
        $("#phoneNumber").setValue("090-1234-5678");
        $("#address").setValue("Tokyo");

        // 4. 編集確認画面へ遷移
        $("button[type=submit]").click();
        $("#page-title").shouldHave(text("Confirm Registration"));

        // 5. 確認画面で入力内容を検証
        $("#confirm-email").shouldHave(text("newuser@example.com"));
        $("#confirm-name").shouldHave(text("New User"));
        $("#confirm-birthDate").shouldHave(text("1990-01-01"));
        $("#confirm-phoneNumber").shouldHave(text("090-1234-5678"));
        $("#confirm-address").shouldHave(text("Tokyo"));

        // Backボタンと登録ボタンの存在確認
        $$("button[type=submit]").findBy(text("Back")).should(exist);
        $$("button[type=submit]").findBy(text("Register")).should(exist);
        
        // 6. 編集完了
        $$("button[type=submit]").findBy(text("Register")).click();
        $("#page-title").shouldHave(text("Registration Complete"));
        
        // 7. データベース検証
        var customer = customerRepository.findByEmail("newuser@example.com");
        assertThat(customer).isPresent();
        assertThat(customer.get().getName()).isEqualTo("New User");
        assertThat(customer.get().getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(customer.get().getPhoneNumber()).isEqualTo("090-1234-5678");
        assertThat(customer.get().getAddress()).isEqualTo("Tokyo");
        assertThat(customer.get().getRole()).isEqualTo(Customer.Role.USER);
    }

    @Test
    @DisplayName("正常系: 管理者顧客詳細が表示される")
    void testShowAdminCustomerDetail() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 管理者顧客詳細へ遷移
        open("/admin/customers/maria.harris@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 3. 顧客詳細の主要要素が表示される
        $("#confirm-email").shouldHave(text("maria.harris@example.com"));
        $("#confirm-name").shouldHave(text("Maria Harris"));
        $("#confirm-registrationDate").shouldHave(text("2024-03-01"));
        $("#confirm-birthDate").shouldHave(text("1989-12-24"));
        $("#confirm-phoneNumber").shouldHave(text("222-333-4444"));
        $("#confirm-address").shouldHave(text("468 Sycamore St"));
        $("#confirm-role").shouldHave(text("USER"));

        $("a[href='/admin/customers/maria.harris@example.com/edit-input']").shouldBe(visible);
        $("a[href='/admin/customers/maria.harris@example.com/delete-confirm']").shouldBe(visible);
        $("form[action='/admin/customers/maria.harris@example.com/lock']").shouldBe(visible);
        $("form[action='/admin/customers/maria.harris@example.com/password-reset']").shouldBe(visible);
        $("a[href='/admin/customers/maria.harris@example.com/activity-timeline']").shouldBe(visible);
    }

    @Test
    @DisplayName("正常系: 管理者顧客編集ができる（入力→確認→完了）")
    void testEditAdminCustomer() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 編集対象の顧客詳細画面へ遷移
        open("/admin/customers/leo.jackson@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 3. 管理者顧客編集画面へ遷移
        $("a[href='/admin/customers/leo.jackson@example.com/edit-input']").click();
        $("#page-title").shouldHave(text("Edit Customer"));

        // 4. 顧客情報を編集
        $("#name").setValue("Leo Jackson Updated");
        $("#birthDate").setValue(withDate(LocalDate.of(1990, 1, 1)));
        $("#phoneNumber").setValue("555-666-7777");
        $("#address").setValue("123 Updated St");

        // 5. 編集確認画面へ遷移
        $("button[type=submit]").click();
        $("#page-title").shouldHave(text("Confirm Edit"));

        // 6. 確認画面で入力内容を検証
        $("#confirm-name").shouldHave(text("Leo Jackson Updated"));
        $("#confirm-birthDate").shouldHave(text("1990-01-01"));
        $("#confirm-phoneNumber").shouldHave(text("555-666-7777"));
        $("#confirm-address").shouldHave(text("123 Updated St"));

        // Backボタンと登録ボタンの存在確認
        $$("button[type=submit]").findBy(text("Back")).should(exist);
        $$("button[type=submit]").findBy(text("Update")).should(exist);
        
        // 7. 編集完了
        $$("button[type=submit]").findBy(text("Update")).click();
        $("#page-title").shouldHave(text("Update Complete"));
        
        // 8. データベース検証
        var customer = customerRepository.findByEmail("leo.jackson@example.com");
        assertThat(customer).isPresent();
        assertThat(customer.get().getName()).isEqualTo("Leo Jackson Updated");
        assertThat(customer.get().getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(customer.get().getPhoneNumber()).isEqualTo("555-666-7777");
        assertThat(customer.get().getAddress()).isEqualTo("123 Updated St");
    }

    @Test
    @DisplayName("正常系: 管理者顧客削除ができる（確認→完了）")
    void testDeleteAdminCustomer() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 削除対象の顧客詳細画面へ遷移
        open("/admin/customers/karen.thomas@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 3. 削除確認画面へ遷移
        $("a[href='/admin/customers/karen.thomas@example.com/delete-confirm']").click();
        $("#page-title").shouldHave(text("Delete Customer"));

        // 4. 削除実行
        $("button[type=submit]").click();

        // 5. 削除完了画面の検証
        $("#page-title").shouldHave(text("Delete Complete"));

        // 6. データベース検証
        var customer = customerRepository.findByEmail("karen.thomas@example.com");
        assertThat(customer).isNotPresent();
    }

    @Test
    @DisplayName("正常系: 管理者顧客ロックができる")
    void testLockAdminCustomer() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. ロック対象の顧客詳細画面へ遷移
        open("/admin/customers/jack.anderson@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 3. ロック実行
        $$("button[type=submit]").findBy(text("Lock Account")).click();

        // 4. ロック完了画面の検証
        $("#page-title").shouldHave(text("Lock Complete"));

        // 5. データベース検証
        var loginAttempt = loginAttemptRepository.findByEmail("jack.anderson@example.com");
        assertThat(loginAttempt).isPresent();
        assertThat(loginAttempt.get().getAttemptCount()).isEqualTo(loginAttemptProperties.getMax());

        // 6. アンロック対象の顧客詳細画面へ遷移
        open("/admin/customers/jack.anderson@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 7. アンロック実行
        $$("button[type=submit]").findBy(text("Unlock Account")).click();

        // 8. アンロック完了画面の検証
        $("#page-title").shouldHave(text("Unlock Complete"));

        // 9. データベース検証
        loginAttempt = loginAttemptRepository.findByEmail("jack.anderson@example.com");
        assertThat(loginAttempt).isNotPresent();
    }

    @Test
    @DisplayName("正常系: 管理者パスワードリセットができる")
    void testResetAdminCustomerPassword() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. パスワードリセット対象の顧客詳細画面へ遷移
        open("/admin/customers/iris.taylor@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 3. パスワードリセット実行
        $$("button[type=submit]").findBy(text("Send Reset Link")).click();

        // 4. パスワードリセットリンク送信画面の検証
        $("#page-title").shouldHave(text("Password Reset Link Sent"));
    }

    @Test
    @DisplayName("正常系: 管理者顧客詳細のアクティビティタイムラインが表示される")
    void testShowAdminCustomerActivityTimeline() {
        // 1. ログイン（adminを使用）
        login("admin@example.com", "password");

        // 2. 対象の顧客詳細画面へ遷移
        open("/admin/customers/admin@example.com");
        $("#page-title").shouldHave(text("Customer Detail"));

        // 3. アクティビティタイムラインへ遷移
        $("a[href='/admin/customers/admin@example.com/activity-timeline']").click();
        $("#page-title").shouldHave(text("Activity Timeline"));

        // 4. アクティビティタイムラインの主要要素が表示される
        $("#activity-timeline").shouldBe(visible);
        $$("div#activity-item").findBy(text("ログイン成功")).shouldBe(visible);
        $$("div#activity-item").findBy(text("SUCCESS")).shouldBe(visible);
   }
}