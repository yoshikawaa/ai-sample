package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor // 引数なしコンストラクタを生成
@AllArgsConstructor // 全プロパティをセットするコンストラクタを生成
public class Customer {
    private String email;
    private String password;
    private String name;
    private LocalDate registrationDate;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
}
