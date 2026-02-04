package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 統計検索フォーム（期間指定）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsSearchForm {
    
    /** 開始日 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /** 終了日 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
