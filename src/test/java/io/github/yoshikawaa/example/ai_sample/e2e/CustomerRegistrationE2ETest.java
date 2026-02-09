package io.github.yoshikawaa.example.ai_sample.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.yoshikawaa.example.ai_sample.model.Customer;

import java.time.LocalDate;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.SetValueOptions.withDate;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2E: 顧客登録フロー")
class CustomerRegistrationE2ETest extends E2ETestBase {

    @Test
    @DisplayName("正常系: 入力→確認→登録完了の一連のフローが成功する")
    void testSuccessfulRegistrationFlow() {
        // 1. 入力画面を開く
        open("/register/input");
        $("#page-title").shouldHave(text("Customer Registration"));

        // 2. フォーム入力
        $("#email").setValue("newuser@example.com");
        $("#password").setValue("Password123!");
        $("#confirmPassword").setValue("Password123!");
        $("#name").setValue("New User");
        $("#birthDate").setValue(withDate(LocalDate.of(1990, 1, 1)));
        $("#phoneNumber").setValue("090-1234-5678");
        $("#address").setValue("Tokyo");

        // 3. 確認画面へ遷移
        $("button[type=submit]").click();
        
        // 確認画面のタイトルが表示されるまで待機
        $("#page-title").shouldHave(text("Confirm Registration"));

        // 4. 確認画面の検証
        $("#page-title").shouldHave(text("Confirm Registration"));
        $("#confirm-email").shouldHave(text("newuser@example.com"));
        $("#confirm-name").shouldHave(text("New User"));
        $("#confirm-birthDate").shouldHave(text("1990-01-01"));
        $("#confirm-phoneNumber").shouldHave(text("090-1234-5678"));
        $("#confirm-address").shouldHave(text("Tokyo"));

        // Backボタンと登録ボタンの存在確認
        $$("button[type=submit]").findBy(text("Back")).should(exist);
        $$("button[type=submit]").findBy(text("Register")).should(exist);

        // 5. 登録実行
        $$("button[type=submit]").findBy(text("Register")).click();

        // 6. 完了画面の検証
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
    @DisplayName("異常系: バリデーションエラー時に入力画面にエラーメッセージを表示")
    void testValidationError() {
        // 1. 入力画面を開く
        open("/register/input");

        // 2. 不正なデータで送信
        $("#email").setValue("invalid-email");
        $("#password").setValue("123");
        $("#confirmPassword").setValue("456");
        $("#name").setValue("");
        $("button[type=submit]").click();

        // 3. エラーメッセージ確認（入力画面に戻る）
        $("#page-title").shouldHave(text("Customer Registration"));
        $("#email-error").shouldBe(visible);
        $("#password-error").shouldBe(visible);
        $("#name-error").shouldBe(visible);
    }

    @Test
    @DisplayName("異常系: 未成年の場合はエラー画面を表示")
    void testUnderageCustomerError() {
        // 1. 入力画面を開く
        open("/register/input");

        // 2. 未成年のデータで入力（17歳）
        $("#email").setValue("underage@example.com");
        $("#password").setValue("Password123!");
        $("#confirmPassword").setValue("Password123!");
        $("#name").setValue("Underage User");
        $("#birthDate").setValue(withDate(LocalDate.now().minusYears(17)));
        $("#phoneNumber").setValue("090-9999-9999");
        $("#address").setValue("Tokyo");

        // 3. 確認画面へ
        $("button[type=submit]").click();
        $("#page-title").shouldHave(text("Confirm Registration"));

        // 4. 登録実行
        $$("button[type=submit]").findBy(text("Register")).click();

        // 5. エラー画面の検証
        $("#page-title").shouldHave(text("Error"));
        $("#error-message").shouldBe(visible);
        $("#error-message").shouldHave(text("未成年の登録はできません。"));
    }

    @Test
    @DisplayName("正常系: 確認画面からBackボタンで入力画面に戻れる")
    void testBackToInputFromConfirm() {
        // 1. 確認画面まで進む
        open("/register/input");
        $("#email").setValue("test@example.com");
        $("#password").setValue("Password123!");
        $("#confirmPassword").setValue("Password123!");
        $("#name").setValue("Test User");
        $("#birthDate").setValue(withDate(LocalDate.of(1990, 1, 1)));
        $("#phoneNumber").setValue("090-1234-5678");
        $("#address").setValue("Tokyo");
        $("button[type=submit]").click();

        // 2. 確認画面であることを確認
        $("#page-title").shouldHave(text("Confirm Registration"));

        // 3. Backボタンをクリック
        $$("button[type=submit]").findBy(text("Back")).click();

        // 4. 入力画面に戻り、入力値が保持されている
        $("#page-title").shouldHave(text("Customer Registration"));
        $("#email").shouldHave(value("test@example.com"));
        $("#name").shouldHave(value("Test User"));
        $("#birthDate").shouldHave(value("1990-01-01"));
        $("#phoneNumber").shouldHave(value("090-1234-5678"));
        $("#address").shouldHave(value("Tokyo"));
    }

    @Test
    @DisplayName("異常系: 既に登録済みのメールアドレスの場合は500エラー")
    void testDuplicateEmailError() {
        // 1. 入力画面を開く
        open("/register/input");

        // 2. data.sqlで既に登録済みのメールアドレスを使用
        $("#email").setValue("john.doe@example.com");
        $("#password").setValue("Password123!");
        $("#confirmPassword").setValue("Password123!");
        $("#name").setValue("Duplicate User");
        $("#birthDate").setValue(withDate(LocalDate.of(1990, 1, 1)));
        $("#phoneNumber").setValue("090-1234-5678");
        $("#address").setValue("Tokyo");

        // 3. 確認画面へ
        $("button[type=submit]").click();
        $("#page-title").shouldHave(text("Confirm Registration"));

        // 4. 登録実行
        $$("button[type=submit]").findBy(text("Register")).click();

        // 5. システムエラー画面（500）が表示される
        // DataIntegrityViolationExceptionのハンドリングがないため、ErrorControllerで処理される
        $("#page-title").shouldHave(text("500 - Internal Server Error"));
        $("#error-message").shouldBe(visible);
    }

    @Test
    @DisplayName("異常系: パスワードと確認用パスワードが一致しない場合はエラー")
    void testPasswordMismatchError() {
        // 1. 入力画面を開く
        open("/register/input");

        // 2. パスワードが一致しないデータで送信
        $("#email").setValue("mismatch@example.com");
        $("#password").setValue("Password123!");
        $("#confirmPassword").setValue("Different123!");
        $("#name").setValue("Mismatch User");
        $("#birthDate").setValue(withDate(LocalDate.of(1990, 1, 1)));
        $("#phoneNumber").setValue("090-1234-5678");
        $("#address").setValue("Tokyo");
        $("button[type=submit]").click();

        // 3. エラーメッセージ確認（入力画面に戻る）
        $("#page-title").shouldHave(text("Customer Registration"));
        // confirmPasswordフィールドのエラーメッセージを検証
        $("#confirmPassword-error").shouldBe(visible);
        $("#confirmPassword-error").shouldHave(text("パスワード"));
    }
}
