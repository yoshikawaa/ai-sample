package io.github.yoshikawaa.example.ai_sample.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;
import org.terasoluna.gfw.common.validator.constraints.Compare;

import java.time.LocalDate;

@Data
@Compare(left = "confirmPassword", right = "password", operator = Compare.Operator.EQUAL, message = "パスワードと確認用パスワードが一致しません。")
public class CustomerForm {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "6文字以上で入力してください。")
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    private String name;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd") // 日付フォーマットを指定
    private LocalDate birthDate;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String address;
}
