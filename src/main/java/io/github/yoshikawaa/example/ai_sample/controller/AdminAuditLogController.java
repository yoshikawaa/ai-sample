package io.github.yoshikawaa.example.ai_sample.controller;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import io.github.yoshikawaa.example.ai_sample.model.AuditLogSearchForm;
import io.github.yoshikawaa.example.ai_sample.service.AuditLogService;
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
@RequestMapping("/admin/audit-log")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @ModelAttribute("auditLogSearchForm")
    public AuditLogSearchForm auditLogSearchForm() {
        return new AuditLogSearchForm();
    }

    /**
     * 監査ログ一覧
     */
    @GetMapping
    public String showAuditLog(@PageableDefault(size = 20, sort = "actionTime", direction = Direction.DESC) Pageable pageable,
                                Model model) {
        Page<AuditLog> logPage = auditLogService.getAllAuditLogsWithPagination(pageable);
        model.addAttribute("logPage", logPage);
        return "admin-audit-log";
    }

    /**
     * 監査ログ検索
     */
    @GetMapping("/search")
    public String searchAuditLog(AuditLogSearchForm searchForm,
                                  @PageableDefault(size = 20, sort = "actionTime", direction = Direction.DESC) Pageable pageable,
                                  Model model) {
        // 検索条件が1つでも入力されていれば検索実行
        if (StringUtils.hasText(searchForm.getPerformedBy()) ||
            StringUtils.hasText(searchForm.getTargetEmail()) ||
            searchForm.getActionType() != null ||
            searchForm.getFromDate() != null ||
            searchForm.getToDate() != null) {
            Page<AuditLog> logPage = auditLogService.searchAuditLogsWithPagination(searchForm, pageable);
            model.addAttribute("logPage", logPage);
        } else {
            // 検索条件がない場合は全件表示
            Page<AuditLog> logPage = auditLogService.getAllAuditLogsWithPagination(pageable);
            model.addAttribute("logPage", logPage);
        }
        return "admin-audit-log";
    }
}
