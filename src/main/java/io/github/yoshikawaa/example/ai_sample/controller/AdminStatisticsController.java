package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.StatisticsDto;
import io.github.yoshikawaa.example.ai_sample.model.StatisticsSearchForm;
import io.github.yoshikawaa.example.ai_sample.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

/**
 * 管理者向け統計画面コントローラ
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {
    
    private final StatisticsService statisticsService;
    
    /**
     * すべてのリクエストで使用するフォームオブジェクト（デフォルト値：過去30日）
     */
    @ModelAttribute("statisticsSearchForm")
    public StatisticsSearchForm statisticsSearchForm() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return new StatisticsSearchForm(startDate, endDate);
    }
    
    /**
     * 統計画面を表示（デフォルト期間：過去30日）
     */
    @GetMapping
    public String showStatistics(StatisticsSearchForm statisticsSearchForm, Model model) {
        // フォームは@ModelAttributeメソッドで自動設定される（デフォルト値入り）
        LocalDate startDate = statisticsSearchForm.getStartDate();
        LocalDate endDate = statisticsSearchForm.getEndDate();
        
        // 統計データ取得（1トランザクションでアクセスログも記録）
        StatisticsDto statistics = statisticsService.getStatistics(startDate, endDate);
        
        // StatisticsDtoをそのままModelに追加
        model.addAttribute("statistics", statistics);
        
        log.info("統計画面表示: startDate={}, endDate={}", startDate, endDate);
        
        return "admin-statistics";
    }
    
    /**
     * 期間指定での統計画面表示
     */
    @GetMapping("/search")
    public String searchStatistics(StatisticsSearchForm statisticsSearchForm, Model model) {
        LocalDate startDate = statisticsSearchForm.getStartDate();
        LocalDate endDate = statisticsSearchForm.getEndDate();
        
        // 統計データ取得（1トランザクションでアクセスログも記録）
        StatisticsDto statistics = statisticsService.getStatistics(startDate, endDate);
        
        // StatisticsDtoをそのままModelに追加
        model.addAttribute("statistics", statistics);
        
        log.info("統計画面検索: startDate={}, endDate={}", startDate, endDate);
        
        return "admin-statistics";
    }
}
