package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUnlockToken {
    private String email;
    private String unlockToken;
    private long tokenExpiry;
}
