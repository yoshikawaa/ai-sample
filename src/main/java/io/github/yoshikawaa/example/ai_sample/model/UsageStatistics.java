package io.github.yoshikawaa.example.ai_sample.model;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog.ActionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 利用状況統計データ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatistics {
    
    /** アクションタイプ */
    private ActionType actionType;
    
    /** 件数 */
    private long count;
}
