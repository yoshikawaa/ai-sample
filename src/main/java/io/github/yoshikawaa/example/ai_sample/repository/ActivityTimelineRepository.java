package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline;
import io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline.ActivityType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * アクティビティタイムラインリポジトリ
 * 複数のテーブルから統合的にアクティビティデータを取得
 */
@Mapper
public interface ActivityTimelineRepository {

    /**
     * アクティビティタイムラインを取得（ページネーション・ソート対応）
     * 
     * @param email 顧客メールアドレス
     * @param startDateTime 検索期間開始日時
     * @param endDateTime 検索期間終了日時
     * @param activityTypes フィルタリング対象のアクティビティ種別（nullの場合は全種別）
     * @param limit ページサイズ
     * @param offset オフセット
     * @return アクティビティタイムライン
     */
    @Select("""
        <script>
        SELECT * FROM (
            -- 監査ログ
            SELECT 
                id,
                action_time AS timestamp,
                CASE action_type
                    WHEN 'CREATE' THEN 'ACCOUNT_CREATED'
                    WHEN 'UPDATE' THEN 'INFO_UPDATED'
                    WHEN 'DELETE' THEN 'ACCOUNT_DELETED'
                    WHEN 'PASSWORD_RESET' THEN 'PASSWORD_RESET'
                    WHEN 'ACCOUNT_LOCK' THEN 'ACCOUNT_LOCKED'
                    WHEN 'ACCOUNT_UNLOCK' THEN 'ACCOUNT_UNLOCKED'
                    WHEN 'VIEW_STATISTICS' THEN 'INFO_UPDATED'
                END AS activity_type,
                CASE action_type
                    WHEN 'CREATE' THEN 'アカウント作成'
                    WHEN 'UPDATE' THEN '情報更新'
                    WHEN 'DELETE' THEN 'アカウント削除'
                    WHEN 'PASSWORD_RESET' THEN 'パスワードリセット'
                    WHEN 'ACCOUNT_LOCK' THEN 'アカウントロック'
                    WHEN 'ACCOUNT_UNLOCK' THEN 'アカウントロック解除'
                    WHEN 'VIEW_STATISTICS' THEN '情報更新'
                END AS description,
                action_detail AS detail,
                ip_address,
                NULL AS status
            FROM audit_log
            WHERE target_email = #{email}
              AND action_time BETWEEN #{startDateTime} AND #{endDateTime}
            
            UNION ALL
            
            -- ログイン履歴
            SELECT 
                id,
                login_time AS timestamp,
                CASE status
                    WHEN 'SUCCESS' THEN 'LOGIN_SUCCESS'
                    WHEN 'FAILURE' THEN 'LOGIN_FAILURE'
                    WHEN 'LOCKED' THEN 'ACCOUNT_LOCKED'
                    WHEN 'LOGOUT' THEN 'LOGOUT'
                    WHEN 'SESSION_EXCEEDED' THEN 'SESSION_EXCEEDED'
                END AS activity_type,
                CASE status
                    WHEN 'SUCCESS' THEN 'ログイン成功'
                    WHEN 'FAILURE' THEN 'ログイン失敗'
                    WHEN 'LOCKED' THEN 'アカウントロック'
                    WHEN 'LOGOUT' THEN 'ログアウト'
                    WHEN 'SESSION_EXCEEDED' THEN 'セッション超過'
                END AS description,
                CASE WHEN failure_reason IS NOT NULL THEN CONCAT('失敗理由: ', failure_reason) ELSE NULL END AS detail,
                ip_address,
                status
            FROM login_history
            WHERE email = #{email}
              AND login_time BETWEEN #{startDateTime} AND #{endDateTime}
            
            UNION ALL
            
            -- 通知履歴
            SELECT 
                id,
                sent_at AS timestamp,
                'NOTIFICATION_SENT' AS activity_type,
                CONCAT('通知送信: ', notification_type) AS description,
                CASE 
                    WHEN error_message IS NOT NULL THEN CONCAT('件名: ', subject, '\nエラー: ', error_message)
                    ELSE CONCAT('件名: ', subject)
                END AS detail,
                NULL AS ip_address,
                status
            FROM notification_history
            WHERE recipient_email = #{email}
              AND sent_at BETWEEN #{startDateTime} AND #{endDateTime}
        ) AS unified_timeline
        <where>
            <if test="activityTypes != null and !activityTypes.isEmpty()">
                AND activity_type IN
                <foreach collection="activityTypes" item="type" open="(" separator="," close=")">
                    #{type}
                </foreach>
            </if>
        </where>
        ORDER BY timestamp DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<ActivityTimeline> findActivityTimeline(
        @Param("email") String email,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("activityTypes") List<ActivityType> activityTypes,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * アクティビティタイムラインの総件数を取得
     * 
     * @param email 顧客メールアドレス
     * @param startDateTime 検索期間開始日時
     * @param endDateTime 検索期間終了日時
     * @param activityTypes フィルタリング対象のアクティビティ種別（nullの場合は全種別）
     * @return 総件数
     */
    @Select("""
        <script>
        SELECT COUNT(*) FROM (
            -- 監査ログ
            SELECT 
                id,
                CASE action_type
                    WHEN 'CREATE' THEN 'ACCOUNT_CREATED'
                    WHEN 'UPDATE' THEN 'INFO_UPDATED'
                    WHEN 'DELETE' THEN 'ACCOUNT_DELETED'
                    WHEN 'PASSWORD_RESET' THEN 'PASSWORD_RESET'
                    WHEN 'ACCOUNT_LOCK' THEN 'ACCOUNT_LOCKED'
                    WHEN 'ACCOUNT_UNLOCK' THEN 'ACCOUNT_UNLOCKED'
                    WHEN 'VIEW_STATISTICS' THEN 'INFO_UPDATED'
                END AS activity_type
            FROM audit_log
            WHERE target_email = #{email}
              AND action_time BETWEEN #{startDateTime} AND #{endDateTime}
            
            UNION ALL
            
            -- ログイン履歴
            SELECT 
                id,
                CASE status
                    WHEN 'SUCCESS' THEN 'LOGIN_SUCCESS'
                    WHEN 'FAILURE' THEN 'LOGIN_FAILURE'
                    WHEN 'LOCKED' THEN 'ACCOUNT_LOCKED'
                    WHEN 'LOGOUT' THEN 'LOGOUT'
                    WHEN 'SESSION_EXCEEDED' THEN 'SESSION_EXCEEDED'
                END AS activity_type
            FROM login_history
            WHERE email = #{email}
              AND login_time BETWEEN #{startDateTime} AND #{endDateTime}
            
            UNION ALL
            
            -- 通知履歴
            SELECT 
                id,
                'NOTIFICATION_SENT' AS activity_type
            FROM notification_history
            WHERE recipient_email = #{email}
              AND sent_at BETWEEN #{startDateTime} AND #{endDateTime}
        ) AS unified_timeline
        <where>
            <if test="activityTypes != null and !activityTypes.isEmpty()">
                AND activity_type IN
                <foreach collection="activityTypes" item="type" open="(" separator="," close=")">
                    #{type}
                </foreach>
            </if>
        </where>
        </script>
    """)
    long countActivityTimeline(
        @Param("email") String email,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("activityTypes") List<ActivityType> activityTypes
    );
}
