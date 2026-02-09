package io.github.yoshikawaa.example.ai_sample.e2e;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.yoshikawaa.example.ai_sample.e2e.config.SelenideProperties;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.repository.LoginAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * E2Eテストの共通ベースクラス
 * 
 * 責務:
 * - Spring Bootサーバーの起動（ランダムポート）
 * - Selenide/WebDriverの設定（プロパティ外部化）
 * - GreenMailの起動・停止（動的ポート、本番Configは無効）
 * - データベースのリセット（@Sql）
 * - 共通ヘルパーメソッド
 * 
 * 注意:
 * - 本番のGreenMailConfig（固定ポート3025）はapplication.ymlでapp.greenmail.enabled=falseにより無効化
 * - テスト専用GreenMail（動的ポート）を@BeforeAllで起動し、並行実行に対応
 * - Selenide設定はapp.e2e.selenide.*プロパティで調整可能
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties(SelenideProperties.class)
public abstract class E2ETestBase {

    // ========================================
    // Spring Boot サーバー設定
    // ========================================

    @LocalServerPort
    protected int port;

    protected String baseUrl;

    // ========================================
    // 依存注入
    // ========================================

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected LoginAttemptRepository loginAttemptRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    private SelenideProperties selenideProperties;

    // ========================================
    // GreenMail（テスト用SMTPサーバー）
    // ========================================

    protected static GreenMail greenMail;

    @BeforeAll
    static void setupGreenMail() {
        ServerSetup serverSetup = ServerSetup.SMTP.dynamicPort();
        greenMail = new GreenMail(serverSetup);
        greenMail.setUser("test@example.com", "password");
        greenMail.start();
        log.info("GreenMail started on port: {}", greenMail.getSmtp().getPort());
    }

    @AfterAll
    static void tearDownGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
            log.info("GreenMail stopped");
        }
    }

    // ========================================
    // データベース初期化について
    // ========================================
    
    /**
     * E2Eテストにおけるデータベース初期化:
     * 
     * - DBの初期化はSpring Boot起動時に schema.sql + data.sql が自動実行される
     * - @BeforeEach + @Sql はテストコードのトランザクションで動作するため、
     *   別スレッドで動作するWebアプリケーションのDB接続には影響しない
     * - したがって、テストメソッド間でDB状態が共有される
     * 
     * テストメソッド間の独立性を確保するには:
     * - 各テストメソッドで異なるユーザーを使用すること
     * - 詳細はcopilot-instructions.mdの「E2Eテスト」セクションを参照
     */

    // ========================================
    // Selenide/WebDriver 設定
    // ========================================

    @BeforeEach
    void setupBrowser() {
        // BrowserTypeに応じてWebDriverManagerを設定
        try {
            switch (selenideProperties.getBrowserType()) {
                case EDGE:
                    WebDriverManager.edgedriver()
                        .cachePath("target/.wdm")
                        .setup();
                    break;
                case CHROME:
                    WebDriverManager.chromedriver()
                        .cachePath("target/.wdm")
                        .clearDriverCache()
                        .clearResolutionCache()
                        .setup();
                    break;
                case FIREFOX:
                    WebDriverManager.firefoxdriver()
                        .cachePath("target/.wdm")
                        .clearDriverCache()
                        .clearResolutionCache()
                        .setup();
                    break;
            }
            log.info("WebDriverManager configured for {}", selenideProperties.getBrowserType());
        } catch (Exception e) {
            log.warn("WebDriverManager setup failed, falling back to system driver: {}", e.getMessage());
            // システムにインストールされているドライバーにフォールバック
        }
        
        // Selenide設定（プロパティから取得）
        Configuration.browser = selenideProperties.getBrowserType().getSelenideName();
        Configuration.headless = selenideProperties.isHeadless();
        Configuration.timeout = selenideProperties.getTimeout();
        Configuration.pageLoadTimeout = selenideProperties.getPageLoadTimeout();
        Configuration.screenshots = selenideProperties.isScreenshots();
        Configuration.reportsFolder = "target/selenide-screenshots"; // テストインフラの一部として固定
        Configuration.savePageSource = selenideProperties.isSavePageSource();
        
        log.info("Selenide configured: browser={}, headless={}, timeout={}ms", 
            Configuration.browser, Configuration.headless, Configuration.timeout);
    }

    @BeforeEach
    void setupBaseUrl() {
        baseUrl = "http://localhost:" + port;
        Configuration.baseUrl = baseUrl;
        log.info("Base URL configured: {}", baseUrl);
    }

    @AfterEach
    void cleanupBrowser() {
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        Selenide.closeWebDriver();
        log.debug("Browser cleaned up");
    }

    @AfterEach
    void clearEmails() {
        try {
            greenMail.purgeEmailFromAllMailboxes();
            log.debug("Emails cleared");
        } catch (Exception e) {
            log.warn("Failed to clear emails", e);
        }
    }

    // ========================================
    // 共通ヘルパーメソッド
    // ========================================

    /**
     * ログイン処理
     */
    protected void login(String email, String password) {
        open("/login");
        $("#page-title").shouldHave(text("Login"));
        $("#username").setValue(email);
        $("#password").setValue(password);
        $("button[type=submit]").click();
        log.debug("Logged in as: {}", email);
    }

    /**
     * メール受信数を取得
     */
    protected int getReceivedEmailCount() {
        return greenMail.getReceivedMessages().length;
    }

    /**
     * 最後に受信したメールの件名を取得
     */
    protected String getLastEmailSubject() throws Exception {
        var messages = greenMail.getReceivedMessages();
        if (messages.length == 0) {
            return null;
        }
        return messages[messages.length - 1].getSubject();
    }

    /**
     * 最後に受信したメールの本文を取得
     */
    protected String getLastEmailBody() throws Exception {
        var messages = greenMail.getReceivedMessages();
        if (messages.length == 0) {
            return null;
        }
        return messages[messages.length - 1].getContent().toString();
    }

    /**
     * スクリーンショット取得（デバッグ用）
     */
    protected void takeScreenshot(String fileName) {
        Selenide.screenshot(fileName);
        log.info("Screenshot taken: {}", fileName);
    }
}
