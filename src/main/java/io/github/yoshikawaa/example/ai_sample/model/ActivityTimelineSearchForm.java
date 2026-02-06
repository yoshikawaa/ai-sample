package io.github.yoshikawaa.example.ai_sample.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * アクティビティタイムライン検索フォーム
 * 顧客のアクティビティをフィルタリングするためのフォーム
 */
@Data
public class ActivityTimelineSearchForm {
    
    /**
     * フィルタリング対象のアクティビティ種別（複数選択可）
     */
    private List<ActivityTimeline.ActivityType> activityTypes;
    
    /**
     * 検索期間開始日
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 検索期間終了日
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
