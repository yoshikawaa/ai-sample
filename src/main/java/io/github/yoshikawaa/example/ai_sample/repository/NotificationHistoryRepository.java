package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.NotificationHistory;
import io.github.yoshikawaa.example.ai_sample.model.NotificationTypeCount;
import io.github.yoshikawaa.example.ai_sample.model.StatusCount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知履歴リポジトリ
 */
@Mapper
public interface NotificationHistoryRepository {

    // ========================================
    // 全件取得系
    // ========================================

    /**
     * 全件取得（ページネーション、ソート対応）
     */
    @Select("""
        <script>
        SELECT * FROM notification_history
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY sent_at DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<NotificationHistory> findAllWithPagination(@Param("limit") int limit,
                                                      @Param("offset") int offset,
                                                      @Param("sortColumn") String sortColumn,
                                                      @Param("sortDirection") String sortDirection);

    /**
     * 全件数取得
     */
    @Select("SELECT COUNT(*) FROM notification_history")
    long count();

    // ========================================
    // 検索系
    // ========================================

    /**
     * 検索（ページネーション、ソート対応）
     */
    @Select("""
        <script>
        SELECT * FROM notification_history
        <where>
            <if test="recipientEmail != null and recipientEmail != ''">
                AND LOWER(recipient_email) LIKE LOWER(CONCAT('%', #{recipientEmail}, '%'))
            </if>
            <if test="notificationType != null">
                AND notification_type = #{notificationType}
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
            <if test="startDate != null">
                AND sent_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND sent_at &lt; #{endDate}
            </if>
        </where>
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY sent_at DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<NotificationHistory> searchWithPagination(@Param("recipientEmail") String recipientEmail,
                                                     @Param("notificationType") NotificationHistory.NotificationType notificationType,
                                                     @Param("status") NotificationHistory.Status status,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset,
                                                     @Param("sortColumn") String sortColumn,
                                                     @Param("sortDirection") String sortDirection);

    /**
     * 検索件数取得
     */
    @Select("""
        <script>
        SELECT COUNT(*) FROM notification_history
        <where>
            <if test="recipientEmail != null and recipientEmail != ''">
                AND LOWER(recipient_email) LIKE LOWER(CONCAT('%', #{recipientEmail}, '%'))
            </if>
            <if test="notificationType != null">
                AND notification_type = #{notificationType}
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
            <if test="startDate != null">
                AND sent_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND sent_at &lt; #{endDate}
            </if>
        </where>
        </script>
    """)
    long countBySearch(@Param("recipientEmail") String recipientEmail,
                       @Param("notificationType") NotificationHistory.NotificationType notificationType,
                       @Param("status") NotificationHistory.Status status,
                       @Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    // ========================================
    // 単一取得
    // ========================================

    /**
     * IDで取得
     */
    @Select("SELECT * FROM notification_history WHERE id = #{id}")
    NotificationHistory findById(Long id);

    // ========================================
    // 登録
    // ========================================

    /**
     * 通知履歴を登録
     */
    @Insert("""
        INSERT INTO notification_history (recipient_email, notification_type, subject, body, status, error_message, sent_at, created_at)
        VALUES (#{recipientEmail}, #{notificationType}, #{subject}, #{body}, #{status}, #{errorMessage}, #{sentAt}, #{createdAt})
    """)
    void insert(NotificationHistory notificationHistory);

    // ========================================
    // 統計
    // ========================================

    /**
     * 通知種別ごとの送信数を取得
     */
    @Select("""
        SELECT notification_type, COUNT(*) as count
        FROM notification_history
        GROUP BY notification_type
        ORDER BY count DESC
    """)
    List<NotificationTypeCount> countByNotificationType();

    /**
     * ステータスごとの送信数を取得
     */
    @Select("""
        SELECT status, COUNT(*) as count
        FROM notification_history
        GROUP BY status
    """)
    List<StatusCount> countByStatus();

    // ========================================
    // 顧客アクティビティタイムライン用
    // ========================================

    /**
     * 特定顧客の通知履歴を取得（タイムライン用）
     */
    @Select("""
        <script>
        SELECT * FROM notification_history
        WHERE recipient_email = #{email}
        <if test="startDate != null">
            AND sent_at &gt;= #{startDate}
        </if>
        <if test="endDate != null">
            AND sent_at &lt; #{endDate}
        </if>
        ORDER BY sent_at DESC
        LIMIT #{limit}
        </script>
    """)
    List<NotificationHistory> findByRecipientEmail(@Param("email") String email,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     @Param("limit") int limit);
}
