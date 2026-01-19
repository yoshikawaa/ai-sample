package io.github.yoshikawaa.example.ai_sample.model;

import org.terasoluna.gfw.common.validator.constraints.Compare;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Compare(left = "confirmPassword", right = "newPassword", operator = Compare.Operator.EQUAL, message = "新しいパスワードと確認用パスワードが一致しません。")
public class PasswordResetForm {

    private String token;

    @NotBlank
    @Size(min = 6, message = "6文字以上で入力してください。")
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}