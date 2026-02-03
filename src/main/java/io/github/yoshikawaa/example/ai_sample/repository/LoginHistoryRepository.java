package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.LoginHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LoginHistoryRepository {
    // ========================================
    // 登録
    // ========================================
    
    @Insert("""
        INSERT INTO login_history (email, login_time, status, ip_address, user_agent, failure_reason)
        VALUES (#{email}, #{loginTime}, #{status}, #{ipAddress}, #{userAgent}, #{failureReason})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(LoginHistory loginHistory);

    // ========================================
    // 全件取得系
    // ========================================
    
    @Select("""
        <script>
        SELECT * FROM login_history
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY login_time DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<LoginHistory> findAllWithPagination(@Param("limit") int limit, @Param("offset") int offset,
                                               @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("SELECT COUNT(*) FROM login_history")
    long count();

    // ========================================
    // 検索系
    // ========================================

    @Select("""
        <script>
        SELECT * FROM login_history
        <where>
            <if test="email != null and email != ''">
                AND LOWER(email) LIKE LOWER(CONCAT('%', #{email}, '%'))
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="fromDate != null">
                AND CAST(login_time AS DATE) &gt;= #{fromDate}
            </if>
            <if test="toDate != null">
                AND CAST(login_time AS DATE) &lt;= #{toDate}
            </if>
        </where>
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY login_time DESC, id DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<LoginHistory> searchWithPagination(@Param("email") String email, @Param("status") String status,
                                              @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                              @Param("limit") int limit, @Param("offset") int offset,
                                              @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("""
        <script>
        SELECT COUNT(*) FROM login_history
        <where>
            <if test="email != null and email != ''">
                AND LOWER(email) LIKE LOWER(CONCAT('%', #{email}, '%'))
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="fromDate != null">
                AND CAST(login_time AS DATE) &gt;= #{fromDate}
            </if>
            <if test="toDate != null">
                AND CAST(login_time AS DATE) &lt;= #{toDate}
            </if>
        </where>
        </script>
    """)
    long countBySearch(@Param("email") String email, @Param("status") String status,
                       @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
