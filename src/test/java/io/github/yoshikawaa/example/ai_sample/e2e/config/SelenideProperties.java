package io.github.yoshikawaa.example.ai_sample.e2e.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Selenide/WebDriver設定のプロパティクラス
 */
@Data
@ConfigurationProperties(prefix = "app.e2e.selenide")
public class SelenideProperties {

    /** ブラウザ種別 */
    private BrowserType browserType = BrowserType.EDGE;

    /** ヘッドレスモード */
    private boolean headless = true;

    /** 要素待機タイムアウト（ミリ秒） */
    private long timeout = 10000;

    /** ページロードタイムアウト（ミリ秒） */
    private long pageLoadTimeout = 30000;

    /** スクリーンショット取得の有効化 */
    private boolean screenshots = true;

    /** ページソース保存の有効化 */
    private boolean savePageSource = false;

    /**
     * 対応ブラウザの種別
     */
    public static enum BrowserType {
        EDGE("edge"),
        CHROME("chrome"),
        FIREFOX("firefox");

        private final String selenideName;

        BrowserType(String selenideName) {
            this.selenideName = selenideName;
        }

        public String getSelenideName() {
            return selenideName;
        }
    }
}
