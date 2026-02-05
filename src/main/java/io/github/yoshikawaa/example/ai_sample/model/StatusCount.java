package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステータスごとの送信数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusCount {
    private String status;
    private long count;
}
