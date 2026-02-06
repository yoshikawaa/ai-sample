package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline;
import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline.ActivityType;
import io.github.yoshikawaa.example.ai_sample.repository.ActivityTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * アクティビティタイムラインサービス
 * 顧客の活動履歴を統合的に管理
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ActivityTimelineService {

    private final ActivityTimelineRepository activityTimelineRepository;

    /**
     * 顧客のアクティビティタイムラインを取得
     * 
     * @param email 顧客メールアドレス
     * @param startDate 検索期間開始日
     * @param endDate 検索期間終了日
     * @param activityTypes フィルタリング対象のアクティビティ種別（nullの場合は全種別）
     * @param pageable ページネーション情報
     * @return アクティビティタイムライン
     */
    public Page<ActivityTimeline> getActivityTimeline(String email, LocalDate startDate, LocalDate endDate, 
                                                       List<ActivityType> activityTypes, Pageable pageable) {
        log.info("アクティビティタイムライン取得開始: email={}, startDate={}, endDate={}", email, startDate, endDate);
        
        // LocalDateをLocalDateTimeに変換
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // データベース側でソート・フィルタリング・ページネーションを実行
        List<ActivityTimeline> timeline = activityTimelineRepository.findActivityTimeline(
            email, 
            startDateTime, 
            endDateTime, 
            activityTypes,
            pageable.getPageSize(),
            (int) pageable.getOffset()
        );
        
        // 総件数を取得
        long totalCount = activityTimelineRepository.countActivityTimeline(
            email, 
            startDateTime, 
            endDateTime, 
            activityTypes
        );
        
        log.info("アクティビティタイムライン取得完了: email={}, 総件数={}, ページ件数={}", 
            email, totalCount, timeline.size());
        
        return new PageImpl<>(timeline, pageable, totalCount);
    }
}