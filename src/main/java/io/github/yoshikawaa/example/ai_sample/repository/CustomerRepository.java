package io.github.yoshikawaa.example.ai_sample.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import io.github.yoshikawaa.example.ai_sample.model.Customer;

@Mapper
public interface CustomerRepository {
    // ========================================
    // 全件取得系
    // ========================================
    
    @Select("""
        <script>
        SELECT * FROM customer
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY registration_date DESC
            </otherwise>
        </choose>
        </script>
    """)
    List<Customer> findAllWithSort(@Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("""
        <script>
        SELECT * FROM customer
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY registration_date DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<Customer> findAllWithPagination(@Param("limit") int limit, @Param("offset") int offset, 
                                          @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("SELECT COUNT(*) FROM customer")
    long count();

    // ========================================
    // 検索系
    // ========================================

    @Select("""
        <script>
        SELECT * FROM customer
        <where>
            <if test="name != null and name != ''">
                AND LOWER(name) LIKE LOWER(CONCAT('%', #{name}, '%'))
            </if>
            <if test="email != null and email != ''">
                AND LOWER(email) LIKE LOWER(CONCAT('%', #{email}, '%'))
            </if>
        </where>
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY registration_date DESC
            </otherwise>
        </choose>
        </script>
    """)
    List<Customer> searchWithSort(@Param("name") String name, @Param("email") String email,
                                   @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("""
        <script>
        SELECT * FROM customer
        <where>
            <if test="name != null and name != ''">
                AND LOWER(name) LIKE LOWER(CONCAT('%', #{name}, '%'))
            </if>
            <if test="email != null and email != ''">
                AND LOWER(email) LIKE LOWER(CONCAT('%', #{email}, '%'))
            </if>
        </where>
        <choose>
            <when test="sortColumn != null and sortColumn != ''">
                ORDER BY ${sortColumn} ${sortDirection}
            </when>
            <otherwise>
                ORDER BY registration_date DESC
            </otherwise>
        </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
    """)
    List<Customer> searchWithPagination(@Param("name") String name, @Param("email") String email, 
                                         @Param("limit") int limit, @Param("offset") int offset,
                                         @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

    @Select("""
        <script>
        SELECT COUNT(*) FROM customer
        <where>
            <if test="name != null and name != ''">
                AND LOWER(name) LIKE LOWER(CONCAT('%', #{name}, '%'))
            </if>
            <if test="email != null and email != ''">
                AND LOWER(email) LIKE LOWER(CONCAT('%', #{email}, '%'))
            </if>
        </where>
        </script>
    """)
    long countBySearch(@Param("name") String name, @Param("email") String email);

    // ========================================
    // 単一取得
    // ========================================

    @Select("SELECT * FROM customer WHERE email = #{email}")
    Optional<Customer> findByEmail(String email);

    // ========================================
    // 登録
    // ========================================

    @Insert("""
        INSERT INTO customer (email, password, name, registration_date, birth_date, phone_number, address)
        VALUES (#{email}, #{password}, #{name}, #{registrationDate}, #{birthDate}, #{phoneNumber}, #{address})
    """)
    void save(Customer customer);

    // ========================================
    // 更新
    // ========================================

    @Update("""
        UPDATE customer
        SET password = #{password}
        WHERE email = #{email}
    """)
    void updatePassword(@Param("email") String email, @Param("password") String password);

    @Update("""
        UPDATE customer
        SET name = #{name}, birth_date = #{birthDate}, phone_number = #{phoneNumber}, address = #{address}
        WHERE email = #{email}
    """)
    void updateCustomerInfo(Customer customer);

    // ========================================
    // 削除
    // ========================================

    @Update("DELETE FROM customer WHERE email = #{email}")
    void deleteByEmail(String email);
}
