package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
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

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/login-history")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @ModelAttribute("loginHistorySearchForm")
    public LoginHistorySearchForm loginHistorySearchForm() {
        return new LoginHistorySearchForm();
    }

    /**
     * ログイン履歴一覧
     */
    @GetMapping
    public String showLoginHistory(@PageableDefault(size = 20, sort = "loginTime", direction = Direction.DESC) Pageable pageable,
                                    Model model) {
        Page<LoginHistory> historyPage = loginHistoryService.getAllLoginHistoriesWithPagination(pageable);
        model.addAttribute("historyPage", historyPage);
        return "admin-login-history";
    }

    /**
     * ログイン履歴検索
     */
    @GetMapping("/search")
    public String searchLoginHistory(LoginHistorySearchForm searchForm,
                                      @PageableDefault(size = 20, sort = "loginTime", direction = Direction.DESC) Pageable pageable,
                                      Model model) {
        // 検索条件が1つでも入力されていれば検索実行
        if (StringUtils.hasText(searchForm.getEmail()) || 
            StringUtils.hasText(searchForm.getStatus()) ||
            searchForm.getFromDate() != null ||
            searchForm.getToDate() != null) {
            Page<LoginHistory> historyPage = loginHistoryService.searchLoginHistoriesWithPagination(searchForm, pageable);
            model.addAttribute("historyPage", historyPage);
        } else {
            // 検索条件がない場合は全件表示
            Page<LoginHistory> historyPage = loginHistoryService.getAllLoginHistoriesWithPagination(pageable);
            model.addAttribute("historyPage", historyPage);
        }
        return "admin-login-history";
    }
}
