package io.github.yoshikawaa.example.ai_sample.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import io.github.yoshikawaa.example.ai_sample.model.LoginAttempt;

@Mapper
public interface LoginAttemptRepository {

    @Select("SELECT * FROM login_attempt WHERE email = #{email}")
    Optional<LoginAttempt> findByEmail(String email);

    @Insert("""
        INSERT INTO login_attempt (email, attempt_count, locked_until, last_attempt_time)
        VALUES (#{email}, #{attemptCount}, #{lockedUntil}, #{lastAttemptTime})
    """)
    void insert(LoginAttempt loginAttempt);

    @Update("""
        UPDATE login_attempt
        SET attempt_count = #{attemptCount}, locked_until = #{lockedUntil}, last_attempt_time = #{lastAttemptTime}
        WHERE email = #{email}
    """)
    void update(LoginAttempt loginAttempt);

    @Update("DELETE FROM login_attempt WHERE email = #{email}")
    void deleteByEmail(String email);
}
