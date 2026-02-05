package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.service.NotificationHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 管理者向け通知履歴画面コントローラ
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/notification-history")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationHistoryController {

    private final NotificationHistoryService notificationHistoryService;

    @ModelAttribute("notificationHistorySearchForm")
    public NotificationHistorySearchForm notificationHistorySearchForm() {
        return new NotificationHistorySearchForm();
    }

    /**
     * 通知履歴一覧
     */
    @GetMapping
    public String showNotificationHistory(@PageableDefault(size = 20, sort = "sentAt", direction = Direction.DESC) Pageable pageable,
                                           Model model) {
        Page<NotificationHistory> historyPage = notificationHistoryService.getAllNotificationHistoriesWithPagination(pageable);
        NotificationHistoryService.NotificationHistoryStatistics statistics = notificationHistoryService.getStatistics();
        
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("statistics", statistics);
        
        return "admin-notification-history";
    }

    /**
     * 通知履歴検索
     */
    @GetMapping("/search")
    public String searchNotificationHistory(NotificationHistorySearchForm searchForm,
                                             @PageableDefault(size = 20, sort = "sentAt", direction = Direction.DESC) Pageable pageable,
                                             Model model) {
        // 検索条件が入力されているかチェック
        boolean hasSearchCriteria = StringUtils.hasText(searchForm.getRecipientEmail()) ||
                                      searchForm.getNotificationType() != null ||
                                      searchForm.getStatus() != null ||
                                      searchForm.getStartDate() != null ||
                                      searchForm.getEndDate() != null;

        // 検索条件がない場合は一覧表示にリダイレクト
        if (!hasSearchCriteria) {
            return "redirect:/admin/notification-history";
        }

        Page<NotificationHistory> historyPage = notificationHistoryService.searchNotificationHistoriesWithPagination(searchForm, pageable);
        NotificationHistoryService.NotificationHistoryStatistics statistics = notificationHistoryService.getStatistics();
        
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("statistics", statistics);
        
        return "admin-notification-history";
    }
}
