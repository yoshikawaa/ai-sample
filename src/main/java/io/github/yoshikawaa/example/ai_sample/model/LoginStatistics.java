package io.github.yoshikawaa.example.ai_sample.model;

import io.github.yoshikawaa.example.ai_sample.model.LoginHistory.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログイン統計データ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginStatistics {
    
    /** ログインステータス */
    private Status status;
    
    /** 件数 */
    private long count;
}
