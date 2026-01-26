package io.github.yoshikawaa.example.ai_sample.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * カスタムエラーコントローラー
 * HTTPステータスエラー（404、500等）を処理し、適切なエラーページを表示します
 */
@Slf4j
@Controller
public class CustomErrorController extends AbstractErrorController {

    public CustomErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    /**
     * エラーハンドリングのメインエンドポイント
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        HttpStatus status = getStatus(request);
        Map<String, Object> errorAttributes = getErrorAttributes(request, 
            ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.EXCEPTION,
                ErrorAttributeOptions.Include.BINDING_ERRORS
            ));

        // 詳細なログ記録（将来のロギング機能強化に対応）
        String path = (String) errorAttributes.get("path");
        String message = (String) errorAttributes.get("message");
        String exceptionType = (String) errorAttributes.get("exception");

        if (status.is5xxServerError()) {
            log.error("Server error occurred: status={}, path={}, message={}, exception={}", 
                status.value(), path, message, exceptionType);
        } else if (status == HttpStatus.NOT_FOUND) {
            // 404エラーはINFOレベル（favicon等の正常な動作とユーザーの誤操作の両方を記録）
            log.info("Resource not found: path={}", path);
        } else {
            log.warn("Client error: status={}, path={}, message={}", 
                status.value(), path, message);
        }

        // モデルにエラー情報を追加
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        model.addAttribute("path", path);

        // ステータスコードに応じたテンプレート選択
        if (status == HttpStatus.NOT_FOUND) {
            return "error/404";
        } else if (status.is5xxServerError()) {
            return "error/500";
        }
        
        // その他のエラーは汎用エラーページ（error.html）
        return "error";
    }
}
