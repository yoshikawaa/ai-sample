package io.github.yoshikawaa.example.ai_sample.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * E2E: パスワードリセット機能のテスト
 *
 * 注意: 各テストメソッドは異なるユーザーを使用してテストメソッド間の独立性を確保
 */
@DisplayName("E2E: パスワードリセット機能")
@ExtendWith(OutputCaptureExtension.class)
public class PasswordResetE2ETest extends E2ETestBase {

    @Test
    @DisplayName("正常系: パスワードリセットメールの送信とパスワード変更ができる")
    void testPasswordResetFlow(CapturedOutput capturedOutput) throws Exception {
        // 1. パスワードリセットリクエスト画面へ遷移
        open("/password-reset/request");
        $("#page-title").shouldHave(text("Password Reset Request"));

        // 2. パスワードリセットメール送信フォームにメールアドレスを入力
        $("#email").setValue("john.doe@example.com");
        $("button[type=submit]").click();

        // 3. パスワードリセットメールが送信されたことを確認
        $("#page-title").shouldHave(text("Check Your Email"));

        String output = capturedOutput.getOut();
        // ログからパスワードリセットリンクを抽出（例: http://localhost:8080/password-reset?token=xxxx)
        String resetLink = extractResetLinkFromEmail(output);

        // 4. パスワードリセットリンクをクリックしてパスワード変更画面へ遷移
        open(resetLink);

        // 5. 新しいパスワードを入力して変更
        $("#page-title").shouldHave(text("Password Reset"));
        $("#newPassword").setValue("newSecurePassword123");
        $("#confirmPassword").setValue("newSecurePassword123");
        $("button[type=submit]").click();

        // 6. パスワード変更が完了したことを確認
        $("#page-title").shouldHave(text("Password Reset Complete"));
    }

    // メール本文からパスワードリセットリンクを抽出するユーティリティメソッド
    private String extractResetLinkFromEmail(String emailBody) {
        int linkStart = emailBody.indexOf("http://localhost:8080");
        int linkEnd = emailBody.indexOf("\n", linkStart);
        if (linkStart != -1) {
            String resetLink = emailBody.substring(linkStart, linkEnd != -1 ? linkEnd : emailBody.length()).trim();
            // リセットリンクからホスト・ポート部分を除去して相対パスに変換
            return resetLink.replace("http://localhost:8080", "");
        }
        throw new IllegalArgumentException("Reset link not found in email body.");
    }
}