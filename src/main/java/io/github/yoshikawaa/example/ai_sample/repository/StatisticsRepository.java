package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.CustomerStatistics;
import io.github.yoshikawaa.example.ai_sample.model.LoginStatistics;
import io.github.yoshikawaa.example.ai_sample.model.UsageStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 統計情報リポジトリ
 */
@Mapper
public interface StatisticsRepository {
    
    // ========================================
    // 顧客数推移
    // ========================================
    
    /**
     * 顧客数推移を取得（日別）
     * 
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 顧客数推移リスト
     */
    @Select("""
        SELECT registration_date as date, COUNT(*) as count
        FROM customer
        WHERE registration_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY registration_date
        ORDER BY date
    """)
    List<CustomerStatistics> getCustomerStatistics(@Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate);
    
    // ========================================
    // ログイン統計
    // ========================================
    
    /**
     * ログイン統計を取得（ステータス別集計）
     * 
     * @param startDate 開始日
     * @param endDate 終了日
     * @return ログイン統計リスト
     */
    @Select("""
        SELECT status, COUNT(*) as count
        FROM login_history
        WHERE CAST(login_time AS DATE) BETWEEN #{startDate} AND #{endDate}
        GROUP BY status
        ORDER BY status
    """)
    List<LoginStatistics> getLoginStatistics(@Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    // ========================================
    // 利用状況統計
    // ========================================
    
    /**
     * 利用状況統計を取得（アクションタイプ別集計）
     * 
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 利用状況統計リスト
     */
    @Select("""
        SELECT action_type, COUNT(*) as count
        FROM audit_log
        WHERE CAST(action_time AS DATE) BETWEEN #{startDate} AND #{endDate}
        GROUP BY action_type
        ORDER BY action_type
    """)
    List<UsageStatistics> getUsageStatistics(@Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
}
