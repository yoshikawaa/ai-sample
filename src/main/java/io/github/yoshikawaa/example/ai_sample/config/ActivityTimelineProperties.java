package io.github.yoshikawaa.example.ai_sample.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * アクティビティタイムライン設定プロパティ
 */
@Data
@ConfigurationProperties(prefix = "app.activity-timeline")
public class ActivityTimelineProperties {
    
    /**
     * 各テーブルから取得する最大件数
     */
    private int fetchLimit = 500;
}
