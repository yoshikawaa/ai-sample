package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知種別ごとの送信数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTypeCount {
    private String notificationType;
    private long count;
}
