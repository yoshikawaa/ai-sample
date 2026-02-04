package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 顧客数推移の統計データ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatistics {
    
    /** 日付 */
    private LocalDate date;
    
    /** 顧客数 */
    private long count;
}
