package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogRepository {
    // ========================================
    // 登録
    // ========================================
    
    @Insert("""
        INSERT INTO audit_log (performed_by, target_email, action_type, action_detail, action_time, ip_address)
        VALUES (#{performedBy}, #{targetEmail}, #{actionType}, #{actionDetail}, #{actionTime}, #{ipAddress})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AuditLog auditLog);

    // ========================================
    // 全件取得系
    // ========================================
    
    @Select("""
        <script>
        SELECT * FROM audit_log
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY action_time DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<AuditLog> findAllWithPagination(@Param("limit") int limit, @Param("offset") int offset,
                                           @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("SELECT COUNT(*) FROM audit_log")
    long count();

    // ========================================
    // 検索系
    // ========================================

    @Select("""
        <script>
        SELECT * FROM audit_log
        <where>
            <if test="performedBy != null and performedBy != ''">
                AND LOWER(performed_by) LIKE LOWER(CONCAT('%', #{performedBy}, '%'))
            </if>
            <if test="targetEmail != null and targetEmail != ''">
                AND LOWER(target_email) LIKE LOWER(CONCAT('%', #{targetEmail}, '%'))
            </if>
            <if test="actionType != null">
                AND action_type = #{actionType}
            </if>
            <if test="fromDate != null">
                AND CAST(action_time AS DATE) &gt;= #{fromDate}
            </if>
            <if test="toDate != null">
                AND CAST(action_time AS DATE) &lt;= #{toDate}
            </if>
        </where>
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY action_time DESC, id DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<AuditLog> searchWithPagination(@Param("performedBy") String performedBy, @Param("targetEmail") String targetEmail,
                                          @Param("actionType") AuditLog.ActionType actionType,
                                          @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                          @Param("limit") int limit, @Param("offset") int offset,
                                          @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("""
        <script>
        SELECT COUNT(*) FROM audit_log
        <where>
            <if test="performedBy != null and performedBy != ''">
                AND LOWER(performed_by) LIKE LOWER(CONCAT('%', #{performedBy}, '%'))
            </if>
            <if test="targetEmail != null and targetEmail != ''">
                AND LOWER(target_email) LIKE LOWER(CONCAT('%', #{targetEmail}, '%'))
            </if>
            <if test="actionType != null">
                AND action_type = #{actionType}
            </if>
            <if test="fromDate != null">
                AND CAST(action_time AS DATE) &gt;= #{fromDate}
            </if>
            <if test="toDate != null">
                AND CAST(action_time AS DATE) &lt;= #{toDate}
            </if>
        </where>
        </script>
    """)
    long countBySearch(@Param("performedBy") String performedBy, @Param("targetEmail") String targetEmail,
                       @Param("actionType") AuditLog.ActionType actionType,
                       @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // ========================================
    // 顧客アクティビティタイムライン用
    // ========================================

    /**
     * 特定顧客の監査ログを取得（タイムライン用）
     */
    @Select("""
        <script>
        SELECT * FROM audit_log
        WHERE target_email = #{email}
        <if test="startDate != null">
            AND action_time &gt;= #{startDate}
        </if>
        <if test="endDate != null">
            AND action_time &lt;= #{endDate}
        </if>
        ORDER BY action_time DESC
        LIMIT #{limit}
        </script>
    """)
    List<AuditLog> findByTargetEmail(@Param("email") String email,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      @Param("limit") int limit);
}
