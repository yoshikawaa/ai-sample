package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.AccountUnlockToken;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountUnlockTokenRepository {

    @Insert("""
        INSERT INTO account_unlock_token (email, unlock_token, token_expiry)
        VALUES (#{email}, #{unlockToken}, #{tokenExpiry})
    """)
    void insert(AccountUnlockToken token);

    @Select("SELECT email, unlock_token AS unlockToken, token_expiry AS tokenExpiry FROM account_unlock_token WHERE unlock_token = #{unlockToken}")
    AccountUnlockToken findByToken(String unlockToken);

    @Delete("DELETE FROM account_unlock_token WHERE unlock_token = #{unlockToken}")
    void deleteByToken(String unlockToken);
}
