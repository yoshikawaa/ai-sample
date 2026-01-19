package io.github.yoshikawaa.example.ai_sample.repository;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import io.github.yoshikawaa.example.ai_sample.model.PasswordResetToken;

@Mapper
public interface PasswordResetTokenRepository {

    @Insert("""
        INSERT INTO password_reset_tokens (email, reset_token, token_expiry)
        VALUES (#{email}, #{resetToken}, #{tokenExpiry})
    """)
    void insert(PasswordResetToken token);

    @Select("SELECT * FROM password_reset_tokens WHERE reset_token = #{resetToken}")
    PasswordResetToken findByResetToken(String resetToken);

    @Delete("DELETE FROM password_reset_tokens WHERE email = #{email}")
    void deleteByEmail(String email);
}
