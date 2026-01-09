package io.github.yoshikawaa.example.ai_sample.model;

import io.github.yoshikawaa.example.ai_sample.validation.CurrentPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.terasoluna.gfw.common.validator.constraints.Compare;

@Data
@Compare(left = "confirmPassword", right = "newPassword", operator = Compare.Operator.EQUAL, message = "新しいパスワードと確認用パスワードが一致しません。")
@Compare(left = "newPassword", right = "currentPassword", operator = Compare.Operator.NOT_EQUAL, message = "新しいパスワードは現在のパスワードと異なる必要があります。")
public class ChangePasswordForm {

    @NotBlank
    @CurrentPassword
    private String currentPassword;

    @NotBlank
    @Size(min = 6, message = "6文字以上で入力してください。")
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
