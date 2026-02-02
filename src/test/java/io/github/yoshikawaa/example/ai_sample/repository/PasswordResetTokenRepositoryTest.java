package io.github.yoshikawaa.example.ai_sample.repository;

import io.github.yoshikawaa.example.ai_sample.model.PasswordResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import io.github.yoshikawaa.example.ai_sample.model.Customer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PasswordResetTokenRepository のテスト")
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        // 各テストの前にtest@example.comの顧客を必ずinsert（外部キー制約対策）
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setPassword("hashed_password");
        customer.setName("Test User");
        customer.setRegistrationDate(LocalDate.of(2023, 1, 1));
        customer.setBirthDate(LocalDate.of(1990, 1, 1));
        customer.setPhoneNumber("000-0000-0000");
        customer.setAddress("Test Address");
        customer.setRole(Customer.Role.USER);
        customerRepository.insert(customer);
    }

    @Test
    @DisplayName("insert: トークンを挿入できる")
    void testInsert() {
        // トークンを作成
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("test@example.com");
        token.setResetToken("test-token-123");
        token.setTokenExpiry(System.currentTimeMillis() + 3600000); // 1時間後

        // トークンを挿入
        passwordResetTokenRepository.insert(token);

        // 挿入されたトークンを取得して検証
        PasswordResetToken savedToken = passwordResetTokenRepository.findByResetToken("test-token-123");
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getEmail()).isEqualTo("test@example.com");
        assertThat(savedToken.getResetToken()).isEqualTo("test-token-123");
        assertThat(savedToken.getTokenExpiry()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("insert: 既存のメールアドレスで上書きできる")
    void testInsert_既存のメールアドレスで上書き() {
        // 最初のトークンを挿入
        PasswordResetToken token1 = new PasswordResetToken();
        token1.setEmail("test@example.com");
        token1.setResetToken("test-token-old");
        token1.setTokenExpiry(System.currentTimeMillis() + 3600000);
        passwordResetTokenRepository.insert(token1);

        // 同じメールアドレスで新しいトークンを挿入（既存のトークンを削除してから挿入）
        passwordResetTokenRepository.deleteByEmail("test@example.com");
        PasswordResetToken token2 = new PasswordResetToken();
        token2.setEmail("test@example.com");
        token2.setResetToken("test-token-new");
        token2.setTokenExpiry(System.currentTimeMillis() + 3600000);
        passwordResetTokenRepository.insert(token2);

        // 古いトークンが存在しないことを確認
        PasswordResetToken oldToken = passwordResetTokenRepository.findByResetToken("test-token-old");
        assertThat(oldToken).isNull();

        // 新しいトークンが存在することを確認
        PasswordResetToken newToken = passwordResetTokenRepository.findByResetToken("test-token-new");
        assertThat(newToken).isNotNull();
        assertThat(newToken.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByResetToken: 存在するトークンを検索できる")
    void testFindByResetToken_存在するトークン() {
        // トークンを挿入
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("test@example.com");
        token.setResetToken("test-token-456");
        token.setTokenExpiry(System.currentTimeMillis() + 3600000);
        passwordResetTokenRepository.insert(token);

        // トークンを検索
        PasswordResetToken foundToken = passwordResetTokenRepository.findByResetToken("test-token-456");

        // 検証
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getEmail()).isEqualTo("test@example.com");
        assertThat(foundToken.getResetToken()).isEqualTo("test-token-456");
    }

    @Test
    @DisplayName("findByResetToken: 存在しないトークンの場合、nullを返す")
    void testFindByResetToken_存在しないトークン() {
        // 存在しないトークンを検索
        PasswordResetToken foundToken = passwordResetTokenRepository.findByResetToken("non-existent-token");

        // 検証
        assertThat(foundToken).isNull();
    }

    @Test
    @DisplayName("deleteByEmail: メールアドレスでトークンを削除できる")
    void testDeleteByEmail() {
        // トークンを挿入
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("test@example.com");
        token.setResetToken("test-token-789");
        token.setTokenExpiry(System.currentTimeMillis() + 3600000);
        passwordResetTokenRepository.insert(token);

        // トークンが存在することを確認
        PasswordResetToken savedToken = passwordResetTokenRepository.findByResetToken("test-token-789");
        assertThat(savedToken).isNotNull();

        // トークンを削除
        passwordResetTokenRepository.deleteByEmail("test@example.com");

        // トークンが削除されたことを確認
        PasswordResetToken deletedToken = passwordResetTokenRepository.findByResetToken("test-token-789");
        assertThat(deletedToken).isNull();
    }

    @Test
    @DisplayName("deleteByEmail: 存在しないメールアドレスでもエラーが発生しない")
    void testDeleteByEmail_存在しないメールアドレス() {
        // 存在しないメールアドレスで削除を試みる（エラーが発生しないことを確認）
        passwordResetTokenRepository.deleteByEmail("non-existent@example.com");

        // 例外が発生しないことを確認（このテストは正常に完了すべき）
    }

    @Test
    @DisplayName("findByResetToken: 有効期限切れのトークンも取得できる")
    void testFindByResetToken_有効期限切れのトークン() {
        // 有効期限切れのトークンを挿入
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail("test@example.com");
        token.setResetToken("expired-token");
        token.setTokenExpiry(System.currentTimeMillis() - 1000); // 過去の時刻
        passwordResetTokenRepository.insert(token);

        // トークンを検索（リポジトリは有効期限をチェックしないため、取得できる）
        PasswordResetToken foundToken = passwordResetTokenRepository.findByResetToken("expired-token");

        // 検証（リポジトリレベルでは有効期限はチェックされない）
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getTokenExpiry()).isLessThan(System.currentTimeMillis());
    }
}
