package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * ログイン成功を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginSuccess(String email, String ipAddress, String userAgent) {
        try {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setEmail(email);
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setStatus(LoginHistory.Status.SUCCESS);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setFailureReason(null);

            loginHistoryRepository.insert(loginHistory);
            log.info("ログイン成功を記録: email={}", email);
        } catch (Exception e) {
            log.error("ログイン履歴の記録に失敗: email={}", email, e);
        }
    }

    /**
     * ログイン失敗を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String email, String ipAddress, String userAgent, String failureReason) {
        try {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setEmail(email);
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setStatus(LoginHistory.Status.FAILURE);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setFailureReason(failureReason);
            
            loginHistoryRepository.insert(loginHistory);
            log.info("ログイン失敗を記録: email={}, reason={}", email, failureReason);
        } catch (Exception e) {
            log.error("ログイン履歴の記録に失敗: email={}", email, e);
        }
    }

    /**
     * アカウントロック時のログインを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginLocked(String email, String ipAddress, String userAgent) {
        try {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setEmail(email);
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setStatus(LoginHistory.Status.LOCKED);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setFailureReason("アカウントがロックされています");
            
            loginHistoryRepository.insert(loginHistory);
            log.info("アカウントロック中のログイン試行を記録: email={}", email);
        } catch (Exception e) {
            log.error("ログイン履歴の記録に失敗: email={}", email, e);
        }
    }

    /**
     * セッション超過を記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSessionExceeded(String email, String ipAddress, String userAgent) {
        try {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setEmail(email);
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setStatus(LoginHistory.Status.SESSION_EXCEEDED);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setFailureReason("最大セッション数超過");
            
            loginHistoryRepository.insert(loginHistory);
            log.info("セッション超過を記録: email={}", email);
        } catch (Exception e) {
            log.error("ログイン履歴の記録に失敗: email={}", email, e);
        }
    }

    /**
     * ログアウトを記録
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogout(String email, String ipAddress, String userAgent) {
        try {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setEmail(email);
            loginHistory.setLoginTime(LocalDateTime.now());
            loginHistory.setStatus(LoginHistory.Status.LOGOUT);
            loginHistory.setIpAddress(ipAddress);
            loginHistory.setUserAgent(userAgent);
            loginHistory.setFailureReason(null);

            loginHistoryRepository.insert(loginHistory);
            log.info("ログアウトを記録: email={}", email);
        } catch (Exception e) {
            log.error("ログアウト履歴の記録に失敗: email={}", email, e);
        }
    }

    /**
     * ログイン履歴を取得（ページネーション対応）
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> getAllLoginHistoriesWithPagination(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        List<LoginHistory> histories = loginHistoryRepository.findAllWithPagination(pageSize, offset, sortInfo[0], sortInfo[1]);
        long total = loginHistoryRepository.count();
        return new PageImpl<>(histories, pageable, total);
    }

    /**
     * ログイン履歴を検索（ページネーション対応）
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> searchLoginHistoriesWithPagination(LoginHistorySearchForm searchForm, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        String[] sortInfo = extractSortInfo(pageable);
        
        List<LoginHistory> histories = loginHistoryRepository.searchWithPagination(
            searchForm.getEmail(),
            searchForm.getStatus(),
            searchForm.getFromDate(),
            searchForm.getToDate(),
            pageSize,
            offset,
            sortInfo[0],
            sortInfo[1]
        );
        
        long total = loginHistoryRepository.countBySearch(
            searchForm.getEmail(),
            searchForm.getStatus(),
            searchForm.getFromDate(),
            searchForm.getToDate()
        );
        
        return new PageImpl<>(histories, pageable, total);
    }

    /**
     * Pageableからソートカラムとソート方向を抽出
     */
    private String[] extractSortInfo(Pageable pageable) {
        String[] sortInfo = new String[2];
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            String direction = order.getDirection().name();
            
            String column = switch (property) {
                case "email" -> "email";
                case "loginTime" -> "login_time";
                case "status" -> "status";
                case "ipAddress" -> "ip_address";
                case "userAgent" -> "user_agent";
                default -> "login_time";
            };
            
            sortInfo[0] = column;
            sortInfo[1] = direction;
        }
        return sortInfo;
    }
}
