package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import io.github.yoshikawaa.example.ai_sample.model.LoginHistorySearchForm;
import io.github.yoshikawaa.example.ai_sample.repository.LoginHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("LoginHistoryService のテスト")
class LoginHistoryServiceTest {

    @MockitoBean
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private LoginHistoryService loginHistoryService;

    // ========================================
    // ログイン履歴記録
    // ========================================

    @Test
    @DisplayName("recordLoginSuccess: ログイン成功を記録できる")
    void testRecordLoginSuccess() {
        // モックの動作を定義
        doNothing().when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し
        loginHistoryService.recordLoginSuccess("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLoginFailure: ログイン失敗を記録できる")
    void testRecordLoginFailure() {
        // モックの動作を定義
        doNothing().when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し
        loginHistoryService.recordLoginFailure("user@example.com", "192.168.1.1", "Mozilla/5.0", "パスワードが誤っています");

        // 検証
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLoginLocked: アカウントロックを記録できる")
    void testRecordLoginLocked() {
        // モックの動作を定義
        doNothing().when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し
        loginHistoryService.recordLoginLocked("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLogout: ログアウトを記録できる")
    void testRecordLogout() {
        // モックの動作を定義
        doNothing().when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し
        loginHistoryService.recordLogout("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordSessionExceeded: セッション超過を記録できる")
    void testRecordSessionExceeded() {
        // モックの動作を定義
        doNothing().when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し
        loginHistoryService.recordSessionExceeded("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLoginSuccess: 例外が発生してもログ記録は失敗しない（エラーログ出力のみ）")
    void testRecordLoginSuccess_ExceptionHandling() {
        // モックの動作を定義: 例外をスロー
        doThrow(new RuntimeException("Database error")).when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し（例外が発生しても正常終了する）
        loginHistoryService.recordLoginSuccess("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証: 呼び出しは成功する（例外がthrowされない）
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLoginFailure: 例外が発生してもログ記録は失敗しない（エラーログ出力のみ）")
    void testRecordLoginFailure_ExceptionHandling() {
        // モックの動作を定義: 例外をスロー
        doThrow(new RuntimeException("Database error")).when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し（例外が発生しても正常終了する）
        loginHistoryService.recordLoginFailure("user@example.com", "192.168.1.1", "Mozilla/5.0", "パスワードが誤っています");

        // 検証: 呼び出しは成功する（例外がthrowされない）
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLoginLocked: 例外が発生してもログ記録は失敗しない（エラーログ出力のみ）")
    void testRecordLoginLocked_ExceptionHandling() {
        // モックの動作を定義: 例外をスロー
        doThrow(new RuntimeException("Database error")).when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し（例外が発生しても正常終了する）
        loginHistoryService.recordLoginLocked("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証: 呼び出しは成功する（例外がthrowされない）
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordLogout: 例外が発生してもログ記録は失敗しない（エラーログ出力のみ）")
    void testRecordLogout_ExceptionHandling() {
        // モックの動作を定義: 例外をスロー
        doThrow(new RuntimeException("Database error")).when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し（例外が発生しても正常終了する）
        loginHistoryService.recordLogout("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証: 呼び出しは成功する（例外がthrowされない）
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    @Test
    @DisplayName("recordSessionExceeded: 例外が発生してもログ記録は失敗しない（エラーログ出力のみ）")
    void testRecordSessionExceeded_ExceptionHandling() {
        // モックの動作を定義: 例外をスロー
        doThrow(new RuntimeException("Database error")).when(loginHistoryRepository).insert(any(LoginHistory.class));

        // サービスメソッドを呼び出し（例外が発生しても正常終了する）
        loginHistoryService.recordSessionExceeded("user@example.com", "192.168.1.1", "Mozilla/5.0");

        // 検証: 呼び出しは成功する（例外がthrowされない）
        verify(loginHistoryRepository, times(1)).insert(any(LoginHistory.class));
    }

    // ========================================
    // 全件取得+ページネーション
    // ========================================

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: ページネーションで全件取得できる")
    void testGetAllLoginHistoriesWithPagination() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user1@example.com", "SUCCESS"),
            createLoginHistory(2L, "user2@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20);
        when(loginHistoryRepository.findAllWithPagination(20, 0, null, null)).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<LoginHistory> page = loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, null, null);
        verify(loginHistoryRepository, times(1)).count();
    }

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: ソート指定で取得できる")
    void testGetAllLoginHistoriesWithPagination_WithSort() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "admin@example.com", "SUCCESS"),
            createLoginHistory(2L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("email").ascending());
        when(loginHistoryRepository.findAllWithPagination(20, 0, "email", "ASC")).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(2L);

        // サービスメソッドを呼び出し
        Page<LoginHistory> page = loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証
        assertThat(page.getContent()).hasSize(2);
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, "email", "ASC");
    }

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: デフォルトソートが適用される")
    void testGetAllLoginHistoriesWithPagination_DefaultSort() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義（ソート指定なし）
        Pageable pageable = PageRequest.of(0, 20);
        when(loginHistoryRepository.findAllWithPagination(20, 0, null, null)).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証: リポジトリにnullが渡される（リポジトリ側でデフォルトソート適用）
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, null, null);
    }

    // ========================================
    // 検索+ページネーション
    // ========================================

    @Test
    @DisplayName("searchLoginHistoriesWithPagination: 検索条件で絞り込みできる")
    void testSearchLoginHistoriesWithPagination() {
        // テストデータ
        LoginHistorySearchForm searchForm = new LoginHistorySearchForm();
        searchForm.setEmail("user@example.com");
        searchForm.setStatus("SUCCESS");

        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20);
        when(loginHistoryRepository.searchWithPagination(
            eq("user@example.com"), eq("SUCCESS"), any(), any(), eq(20), eq(0), any(), any()
        )).thenReturn(histories);
        when(loginHistoryRepository.countBySearch(
            eq("user@example.com"), eq("SUCCESS"), any(), any()
        )).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<LoginHistory> page = loginHistoryService.searchLoginHistoriesWithPagination(searchForm, pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(loginHistoryRepository, times(1)).searchWithPagination(
            eq("user@example.com"), eq("SUCCESS"), any(), any(), eq(20), eq(0), any(), any()
        );
        verify(loginHistoryRepository, times(1)).countBySearch(
            eq("user@example.com"), eq("SUCCESS"), any(), any()
        );
    }

    @Test
    @DisplayName("searchLoginHistoriesWithPagination: 日付範囲で検索できる")
    void testSearchLoginHistoriesWithPagination_WithDateRange() {
        // テストデータ
        LoginHistorySearchForm searchForm = new LoginHistorySearchForm();
        searchForm.setFromDate(LocalDate.of(2024, 1, 1));
        searchForm.setToDate(LocalDate.of(2024, 1, 31));

        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20);
        when(loginHistoryRepository.searchWithPagination(
            any(), any(), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), 
            anyInt(), anyInt(), any(), any()
        )).thenReturn(histories);
        when(loginHistoryRepository.countBySearch(
            any(), any(), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31))
        )).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<LoginHistory> page = loginHistoryService.searchLoginHistoriesWithPagination(searchForm, pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        verify(loginHistoryRepository, times(1)).searchWithPagination(
            any(), any(), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), 
            anyInt(), anyInt(), any(), any()
        );
    }

    @Test
    @DisplayName("searchLoginHistoriesWithPagination: ソート指定で検索できる")
    void testSearchLoginHistoriesWithPagination_WithSort() {
        // テストデータ
        LoginHistorySearchForm searchForm = new LoginHistorySearchForm();
        searchForm.setStatus("SUCCESS");

        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").descending());
        when(loginHistoryRepository.searchWithPagination(
            any(), eq("SUCCESS"), any(), any(), eq(20), eq(0), eq("login_time"), eq("DESC")
        )).thenReturn(histories);
        when(loginHistoryRepository.countBySearch(
            any(), eq("SUCCESS"), any(), any()
        )).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<LoginHistory> page = loginHistoryService.searchLoginHistoriesWithPagination(searchForm, pageable);

        // 検証
        assertThat(page.getContent()).hasSize(1);
        verify(loginHistoryRepository, times(1)).searchWithPagination(
            any(), eq("SUCCESS"), any(), any(), eq(20), eq(0), eq("login_time"), eq("DESC")
        );
    }

    // ========================================
    // プロパティ→カラムマッピング
    // ========================================

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: loginTimeプロパティがlogin_timeカラムにマッピングされる")
    void testPropertyMapping_LoginTime() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("loginTime").ascending());
        when(loginHistoryRepository.findAllWithPagination(20, 0, "login_time", "ASC")).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証: login_timeカラムが使用される
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, "login_time", "ASC");
    }

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: statusプロパティがstatusカラムにマッピングされる")
    void testPropertyMapping_Status() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("status").ascending());
        when(loginHistoryRepository.findAllWithPagination(20, 0, "status", "ASC")).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        Page<LoginHistory> page = loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証: statusがstatusにマッピングされる
        assertThat(page.getContent()).hasSize(1);
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, "status", "ASC");
    }

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: ipAddressプロパティがip_addressカラムにマッピングされる")
    void testPropertyMapping_IpAddress() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("ipAddress").ascending());
        when(loginHistoryRepository.findAllWithPagination(20, 0, "ip_address", "ASC")).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証: ip_addressカラムが使用される
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, "ip_address", "ASC");
    }

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: userAgentプロパティがuser_agentカラムにマッピングされる")
    void testPropertyMapping_UserAgent() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("userAgent").ascending());
        when(loginHistoryRepository.findAllWithPagination(20, 0, "user_agent", "ASC")).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証: user_agentカラムが使用される
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, "user_agent", "ASC");
    }

    @Test
    @DisplayName("getAllLoginHistoriesWithPagination: 不明なプロパティはlogin_timeにフォールバックされる")
    void testPropertyMapping_Unknown() {
        // テストデータ
        List<LoginHistory> histories = Arrays.asList(
            createLoginHistory(1L, "user@example.com", "SUCCESS")
        );

        // モックの動作を定義
        Pageable pageable = PageRequest.of(0, 20, Sort.by("unknownProperty").ascending());
        when(loginHistoryRepository.findAllWithPagination(20, 0, "login_time", "ASC")).thenReturn(histories);
        when(loginHistoryRepository.count()).thenReturn(1L);

        // サービスメソッドを呼び出し
        loginHistoryService.getAllLoginHistoriesWithPagination(pageable);

        // 検証: login_timeカラムが使用される
        verify(loginHistoryRepository, times(1)).findAllWithPagination(20, 0, "login_time", "ASC");
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private LoginHistory createLoginHistory(Long id, String email, String status) {
        LoginHistory history = new LoginHistory();
        history.setId(id);
        history.setEmail(email);
        history.setStatus(LoginHistory.Status.valueOf(status));
        history.setLoginTime(LocalDateTime.now());
        history.setIpAddress("192.168.1.1");
        history.setUserAgent("Mozilla/5.0");
        return history;
    }
}
