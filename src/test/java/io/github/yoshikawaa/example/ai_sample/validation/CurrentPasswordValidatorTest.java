package io.github.yoshikawaa.example.ai_sample.validation;

import io.github.yoshikawaa.example.ai_sample.security.CustomerUserDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("CurrentPasswordValidator のテスト")
class CurrentPasswordValidatorTest {

    @Autowired
    private Validator validator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestForm form;

    @BeforeEach
    void setUp() {
        form = new TestForm();
    }

    @Test
    @DisplayName("testBlankCurrentPassword: 空のパスワードは有効とみなされる")
    void testBlankCurrentPassword() {
        // 現在のパスワードが空
        form.setCurrentPassword("");
    
        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);
    
        // 空の値は有効とみなされるため、バリデーションエラーは発生しない
        assertTrue(violations.isEmpty(), "Expected no violations for blank password, but found: " + violations);
    }
    
    @Test
    @DisplayName("testNullCurrentPassword: nullのパスワードは有効とみなされる")
    void testNullCurrentPassword() {
        // 現在のパスワードが null
        form.setCurrentPassword(null);
    
        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);
    
        // null の値は有効とみなされるため、バリデーションエラーは発生しない
        assertTrue(violations.isEmpty(), "Expected no violations for null password, but found: " + violations);
    }

    @Test
    @DisplayName("testNoAuthentication: 認証情報がない場合はバリデーションエラー")
    void testNoAuthentication() {
        // SecurityContext に認証情報が設定されていない場合
        SecurityContextHolder.clearContext(); // SecurityContext をクリア

        form.setCurrentPassword("password123");

        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);

        // 認証情報が取得できない場合は無効
        assertFalse(violations.isEmpty(), "Expected violations when authentication is missing, but found none.");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"))); // currentPassword にエラーがあることを確認
    }

    @Test
    @DisplayName("testNoPrincipal: Principalがnullの場合はバリデーションエラー")
    void testNoPrincipal() {
        // SecurityContext に認証情報があるが、Principal が null の場合
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(null); // Principal を null に設定
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    
        form.setCurrentPassword("password123");
    
        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);
    
        // Principal が null の場合は無効
        assertFalse(violations.isEmpty(), "Expected violations when principal is null, but found none.");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"))); // currentPassword にエラーがあることを確認
    }

    @Test
    @DisplayName("testPrincipalNotCustomerUserDetails: PrincipalがCustomerUserDetails型でない場合はエラー")
    void testPrincipalNotCustomerUserDetails() {
        // SecurityContext に認証情報があるが、Principal が CustomerUserDetails 型でない場合
        Object invalidPrincipal = "InvalidPrincipal"; // CustomerUserDetails 型ではないオブジェクト
    
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(invalidPrincipal);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    
        form.setCurrentPassword("password123");
    
        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);
    
        // Principal が CustomerUserDetails 型でない場合は無効
        assertFalse(violations.isEmpty(), "Expected violations when principal is not CustomerUserDetails, but found none.");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"))); // currentPassword にエラーがあることを確認
    }

    @Test
    @DisplayName("testInvalidCurrentPassword: 現在のパスワードが間違っている場合はエラー")
    void testInvalidCurrentPassword() {
        // モックの認証情報を設定
        CustomerUserDetails userDetails = Mockito.mock(CustomerUserDetails.class);
        Mockito.when(userDetails.getPassword()).thenReturn(passwordEncoder.encode("password123"));

        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        // 現在のパスワードが間違っている
        form.setCurrentPassword("wrongPassword");

        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty()); // バリデーションエラーがあることを確認
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("currentPassword"))); // currentPassword にエラーがあることを確認
    }

    @Test
    @DisplayName("testValidCurrentPassword: 正しいパスワードの場合はバリデーション成功")
    void testValidCurrentPassword() {
        // モックの認証情報を設定
        CustomerUserDetails userDetails = Mockito.mock(CustomerUserDetails.class);
        Mockito.when(userDetails.getPassword()).thenReturn(passwordEncoder.encode("password123"));

        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        // 正常な入力
        form.setCurrentPassword("password123");

        Set<ConstraintViolation<TestForm>> violations = validator.validate(form);

        assertTrue(violations.isEmpty()); // バリデーションエラーがないことを確認
    }

    // テスト専用のフォームオブジェクトを内部クラスとして定義
    @Data
    private static class TestForm {
        @CurrentPassword
        private String currentPassword;
    }
}
