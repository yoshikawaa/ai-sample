# GitHub Copilot Instructions for AI Sample Project

このプロジェクトは Spring Boot 3.x ベースの顧客管理アプリケーションです。以下のガイドラインに従ってコードを生成・編集してください。

## プロジェクト構成

### 技術スタック
- **Spring Boot**: 3.5.9
- **Java**: 17
- **永続化**: MyBatis 3.5.16（MyBatis Spring Boot Starter 3.0.5）
- **テンプレートエンジン**: Thymeleaf（Spring Boot管理）
- **CSS**: Tailwind CSS latest（CDN経由）
- **セキュリティ**: Spring Security（Spring Boot管理）
- **メール送信**: GreenMail 2.1.8（テスト用）
- **バリデーション**: Bean Validation（Spring Boot管理）+ TERASOLUNA Validation 5.10.0.RELEASE

### パッケージ構成
```
io.github.yoshikawaa.example.ai_sample/
├── config/          # 設定クラス（Security、GreenMail等）
├── controller/      # コントローラ
├── model/          # エンティティとフォームクラス
├── repository/     # MyBatisマッパー
├── security/       # セキュリティ関連（UserDetails実装等）
├── service/        # ビジネスロジック
└── validation/     # カスタムバリデータ
```

## レイヤーアーキテクチャと責務

### リポジトリ層（Repository）
- **責務**: データアクセス層。データベースとのやり取りを担当
- **役割**:
  - SQL実行（SELECT、INSERT、UPDATE、DELETE）
  - データベースとJavaオブジェクトのマッピング
  - データベース操作のインターフェース提供
- **禁止事項**: ビジネスロジックを含めない

### サービス層（Service）
- **責務**: ビジネスロジック層。業務ルールの実装を担当
- **役割**:
  - ビジネスルールの実装（未成年チェック、パスワード暗号化等）
  - トランザクション管理（`@Transactional`）
  - 複数のリポジトリの組み合わせ
  - 認証情報の更新（`SecurityContextHolder`）
- **禁止事項**: HTTPリクエスト/レスポンスに依存しない

### コントローラ層（Controller）
- **責務**: プレゼンテーション層。HTTPリクエストの処理を担当
- **役割**:
  - HTTPリクエストの受付とルーティング
  - リクエストパラメータのバリデーション
  - サービス層の呼び出し
  - ビューへのデータ渡し（Model）
  - HTTPレスポンスの制御（リダイレクト、ビュー名）
- **禁止事項**: ビジネスロジックやデータアクセスロジックを含めない

### テンプレート層（Template）
- **責務**: ビュー層。HTMLの生成とユーザーへの表示を担当
- **役割**:
  - Thymeleafを使ったHTML生成
  - フォームバインディング（`th:field`）
  - エラーメッセージ表示（`th:errors`）
  - 認証情報の表示（`#authentication`）
- **禁止事項**: ビジネスロジックを含めない（条件分岐は表示制御のみ）

## コーディング規約

### 1. 設定クラス（@Configuration）

#### プロパティベースの条件付きBean登録
```java
@Configuration
@ConditionalOnProperty(name = "app.greenmail.enabled", havingValue = "true", matchIfMissing = true)
public class GreenMailConfig {
    // 本番環境用の設定
}
```

**重要**: 
- カスタムプロパティには必ず `app.` プレフィックスを付ける
- `@Profile` より `@ConditionalOnProperty` を優先使用
- `matchIfMissing = true` でデフォルト動作を明示
- カスタムプロパティは `META-INF/additional-spring-configuration-metadata.json` にメタデータを追加し、IDEでの補完とドキュメント表示を可能にする

### 2. リポジトリ層（MyBatis）

#### マッパーインターフェース
```java
@Mapper
public interface CustomerRepository {
    @Select("SELECT * FROM customer WHERE email = #{email}")
    Optional<Customer> findByEmail(String email);

    @Insert("""
        INSERT INTO customer (email, password, name, registration_date, birth_date, phone_number, address)
        VALUES (#{email}, #{password}, #{name}, #{registrationDate}, #{birthDate}, #{phoneNumber}, #{address})
    """)
    void save(Customer customer);

    @Update("""
        UPDATE customer
        SET name = #{name}, birth_date = #{birthDate}, phone_number = #{phoneNumber}, address = #{address}
        WHERE email = #{email}
    """)
    void updateCustomerInfo(Customer customer);
}
```

**重要**:
- アノテーションベースのSQL定義を使用（XMLマッパー不使用）
- 複数行SQLはテキストブロック（`"""`）を使用
- 複数パラメータの場合は `@Param` を使用

### 3. インポートとコードスタイル

#### インポート規約
```java
// ✅ 推奨: 具体的なインポート
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ❌ 禁止: ワイルドカードインポート
import org.springframework.web.bind.annotation.*;
```

**重要**:
- ワイルドカードインポート（`import ....*;`）は使用しない
- 未使用のインポートは削除する
- IDEの自動インポート機能を活用

#### YAML設定ファイル
```yaml
# ✅ 推奨: 特殊文字を含むキーは角括弧で囲む
spring:
  mail:
    properties:
      "[mail.smtp.auth]": false
      "[mail.smtp.starttls.enable]": false

# ❌ 非推奨: 特殊文字をそのまま使用
spring:
  mail:
    properties:
      mail.smtp.auth: false  # 警告が出る
```

**重要**:
- ドット（`.`）やハイフン（`-`）を含むキーは `"[key.name]"` で囲む
- Spring Boot は両方の形式をサポートするが、警告を避けるため角括弧を使用

### 4. サービス層

#### 認証情報の更新
```java
public void updateCustomerInfo(Customer customer) {
    customerRepository.updateCustomerInfo(customer);
    
    // 認証情報を更新（重要！）
    CustomerUserDetails updatedUserDetails = new CustomerUserDetails(customer);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(updatedUserDetails, null, updatedUserDetails.getAuthorities())
    );
}
```

**重要**:
- 顧客情報更新時は必ず `SecurityContextHolder` も更新
- トランザクション管理が必要な場合は `@Transactional` を付与

### 5. コントローラ層

#### リクエストマッピング
```java
@Controller
@RequestMapping("/customers")
public class CustomerController {
    
    @GetMapping
    public String showCustomers() {
        // GET /customers
    }
    
    @GetMapping("/search")
    public String searchCustomers() {
        // GET /customers/search
    }
}
```

**重要**:
- 複数のメソッドで共通するパスはクラスレベルに `@RequestMapping` を付与
- メソッドレベルには相対パスまたはパスなし（空）を指定
- RESTful なURL設計を心がける

#### フォームオブジェクトの使用
```java
@Controller
@RequestMapping("/customers")
public class CustomerController {
    
    @ModelAttribute("customerSearchForm")
    public CustomerSearchForm customerSearchForm() {
        return new CustomerSearchForm();
    }
    
    @GetMapping
    public String showCustomers(Model model) {
        // customerSearchFormは自動的にモデルに追加される
        return "customer-list";
    }
    
    @GetMapping("/search")
    public String searchCustomers(CustomerSearchForm customerSearchForm, Model model) {
        // 引数のフォームオブジェクトには@ModelAttributeは不要
        // Spring MVCが自動的にリクエストパラメータをバインドし、モデルに追加する
        return "customer-list";
    }
}
```

**重要**:
- コントローラレベルでフォームオブジェクトを共通化する場合は `@ModelAttribute` メソッドを実装
- `@ModelAttribute` メソッドは全てのリクエストハンドラーの前に実行され、戻り値を自動的にモデルに追加
- メソッド引数にフォームオブジェクトを使用する場合、`@ModelAttribute` アノテーションは不要
- Spring MVC が自動的にリクエストパラメータをバインドし、モデルに追加する

#### ページネーション
```java
@GetMapping
public String showCustomers(@PageableDefault(size = 10) Pageable pageable,
                            Model model) {
    Page<Customer> customerPage = customerService.getAllCustomersWithPagination(pageable);
    model.addAttribute("customerPage", customerPage);
    return "customer-list";
}

@GetMapping("/search")
public String searchCustomers(CustomerSearchForm customerSearchForm,
                               @PageableDefault(size = 10) Pageable pageable,
                               Model model) {
    Page<Customer> customerPage = customerService.searchCustomersWithPagination(
        customerSearchForm.getName(), customerSearchForm.getEmail(), pageable);
    model.addAttribute("customerPage", customerPage);
    return "customer-list";
}
```

**重要**:
- ページネーションには `Pageable` 引数を使用（`@RequestParam int page` や `@RequestParam int size` は使用しない）
- `@PageableDefault` でデフォルトのページサイズを指定（ページ番号のデフォルトは0）
- Spring MVC が `?page=1&size=20` などのリクエストパラメータを自動的に `Pageable` にバインド
- `PageRequest.of(page, size)` のような手動生成は不要

#### フォームバリデーション
```java
@PostMapping("/edit")
public String updateCustomer(@AuthenticationPrincipal CustomerUserDetails userDetails,
                              @Validated CustomerEditForm customerEditForm,
                              BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        return "customer-edit";  // エラー時は入力画面に戻る
    }
    // 更新処理
    return "redirect:/mypage/edit-complete";
}
```

**重要**:
- `@Validated` でバリデーション実行
- `BindingResult` は `@Validated` の直後に配置
- バリデーションエラー時は入力画面を再表示
- 成功時は PRG パターン（Post-Redirect-Get）を使用

#### メソッドの配置順序
```java
@Controller
@RequestMapping("/customers")
public class CustomerController {
    
    // 1. 一覧表示（デフォルトエンドポイント）
    @GetMapping
    public String showCustomers(...) { }
    
    // 2. 検索（一覧表示の派生機能）
    @GetMapping("/search")
    public String searchCustomers(...) { }
    
    // 3. 詳細表示（パスパラメータを使用）
    @GetMapping("/{email}")
    public String showCustomerDetail(...) { }
}
```

**重要**:
- 機能的に関連するメソッドを近くに配置
- 一覧表示系（一覧、検索）→ 詳細表示の順
- パスパラメータを使うエンドポイントは最後に配置
- CRUD操作は GET → POST → PUT → DELETE の順
- 可読性とメンテナンス性を重視

### 6. モデルクラス

#### フォームクラス
```java
@Data
public class CustomerEditForm {
    @NotBlank
    private String name;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String address;
}
```

**重要**:
- Lombok の `@Data` を使用
- Bean Validation アノテーションでバリデーション定義
- 日付型には `@DateTimeFormat` を付与

#### クラスレベルバリデーション
```java
@Data
@Compare(left = "confirmPassword", right = "password", operator = Compare.Operator.EQUAL, 
         message = "パスワードと確認用パスワードが一致しません。")
public class CustomerForm {
    // フィールド定義
}
```

### 7. テンプレート（Thymeleaf）

#### 基本構成
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Page Title</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@latest/dist/tailwind.min.css" rel="stylesheet">
</head>
<body class="bg-gray-900 min-h-screen flex items-center justify-center">
    <!-- コンテンツ -->
</body>
</html>
```

#### フォーム
```html
<form th:action="@{/mypage/edit}" th:object="${customerEditForm}" method="post" class="space-y-4" novalidate>
    <div>
        <label for="name" class="block text-gray-700 font-medium">Name</label>
        <input type="text" id="name" name="name" th:field="*{name}" 
               class="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring focus:border-blue-300" 
               placeholder="Enter name" required>
        <p class="text-red-500 text-sm mt-1" th:if="${#fields.hasErrors('name')}" th:errors="*{name}"></p>
    </div>
    
    <div class="text-center mt-6">
        <div class="flex justify-center space-x-4">
            <a th:href="@{/mypage}" class="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600">Cancel</a>
            <button type="submit" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Update</button>
        </div>
    </div>
</form>
```

**重要**:
- Tailwind CSS のユーティリティクラスを使用
- `th:field` でフォームバインディング
- エラーメッセージは `th:errors` で表示
- CSRFトークンは Spring Security が自動挿入
- リンクは必ず `th:href="@{/path}"` を使用（`href="/path"` は禁止）

#### 確認画面パターン
登録・編集などの重要な操作には確認画面を設ける：

```html
<!-- 入力画面: 確認画面へPOST -->
<form th:action="@{/mypage/edit-confirm}" th:object="${customerEditForm}" method="post">
    <!-- 入力フィールド -->
    <button type="submit">Confirm</button>
</form>

<!-- 確認画面: 読み取り専用で表示 -->
<form th:object="${customerEditForm}">
    <input type="text" th:field="*{name}" readonly>
</form>
<!-- Backボタン: 入力画面へ戻る（hiddenフィールドで値を保持） -->
<form th:action="@{/mypage/edit}" th:object="${customerEditForm}" method="post">
    <input type="hidden" th:field="*{name}">
    <button type="submit">Back</button>
</form>
<!-- 登録/更新ボタン: 実行処理へ -->
<form th:action="@{/mypage/update}" th:object="${customerEditForm}" method="post">
    <input type="hidden" th:field="*{name}">
    <button type="submit">Update</button>
</form>
```

**重要**:
- 入力→確認→完了の3画面フロー
- 確認画面からBackボタンで入力画面に戻れる
- hiddenフィールドで入力値を保持
- 同じオブジェクトを複数の画面で使い回す場合は、各要素（`<form>`、`<div>`等）に `th:object="${objectName}"` を指定し、フィールドは `th:field="*{fieldName}"` でバインディング
- `th:object` はフォームに限らず任意のHTML要素で使用可能
- `th:object` を指定することで、Backボタンや確認画面でもバインディングが正しく機能する

#### 完了画面
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Complete</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@latest/dist/tailwind.min.css" rel="stylesheet">
</head>
<body class="bg-gray-900 min-h-screen flex items-center justify-center">
    <div class="container mx-auto px-4">
        <div class="bg-white shadow-md rounded-lg p-8 text-center">
            <h1 class="text-3xl font-bold text-gray-800 mb-6">Complete</h1>
            <p class="text-lg text-gray-600 mb-6">Success message here.</p>
            <div class="flex justify-center space-x-4">
                <a th:href="@{/path}" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Back</a>
            </div>
        </div>
    </div>
</body>
</html>
```

**重要**:
- 完了画面は統一されたデザインを使用
- メッセージは `text-lg text-gray-600`
- ボタンは `flex justify-center space-x-4` で配置
- ボタンのパディングは `px-4 py-2`

#### 画面の統一ルール

**完了画面のフォーマット**:
```html
<div class="mb-6">
    <svg class="mx-auto h-16 w-16 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
    </svg>
</div>
<h1 class="text-3xl font-bold text-gray-800 mb-6">Title in English</h1>
<p class="text-lg text-gray-600 mb-6">
    日本語での説明文。
</p>
<div class="flex justify-center space-x-4">
    <a th:href="@{/path}" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Button in English</a>
</div>
```

**エラー画面のフォーマット**:
```html
<div class="mb-6">
    <svg class="mx-auto h-16 w-16 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
    </svg>
</div>
<h1 class="text-3xl font-bold text-gray-800 mb-6">Error</h1>
<div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6" role="alert" th:text="${errorMessage}">
    Error message here.
</div>
<div class="flex justify-center space-x-4">
    <a th:href="@{/path}" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Button in English</a>
</div>
```

**画面全体の統一ルール**:
- タイトル（h1）: 英語
- 説明文（p）: 日本語
- ボタンテキスト: 英語
- 完了画面: 緑色のチェックアイコン
- エラー画面: 赤色の×マークアイコン

**ボタンの色使い**:
- **プライマリアクション**: `bg-blue-500 hover:bg-blue-600`（検索、登録、更新、削除実行など）
- **セカンダリアクション**: `bg-gray-500 hover:bg-gray-600`（Back、Cancel、戻るなど）
- **新規作成**: `bg-green-500 hover:bg-green-600`（必要に応じて使用）
- **危険な操作**: `bg-red-500 hover:bg-red-600`（削除の確認ボタンなど）

**アイコン表示について**:
- アイコンは **Heroicons** のインラインSVGを使用
- 外部ライブラリやCDN不要（SVGをHTML内に直接記述）
- TailwindCSSクラスでサイズと色を制御: `h-16 w-16 text-green-500` または `text-red-500`
- 完了画面: チェックマーク付き円（`M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z`）
- エラー画面: ×マーク付き円（`M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z`）

## テストコード

### 1. テスト用設定

#### application.yml（src/test/resources）
```yaml
app:
  greenmail:
    enabled: false
```

**重要**:
- テスト用の設定ファイルを作成
- 本番用Beanを無効化する設定を記述

#### テスト用Bean定義
```java
@TestConfiguration
static class TestConfig {
    @Bean
    @Primary
    public GreenMail greenMail() {
        // 動的ポートでGreenMailを起動
        ServerSetup serverSetup = new ServerSetup(0, "localhost", "smtp");
        GreenMail greenMail = new GreenMail(serverSetup);
        greenMail.start();
        greenMail.setUser("test@example.com", "password");
        return greenMail;
    }
}
```

**重要**:
- `@TestConfiguration` + `@Primary` でテスト用Beanをオーバーライド
- `@ActiveProfiles` は使用しない（不要）
- テストクラス内に static inner class として定義

### 2. コントローラテスト

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("顧客情報編集機能のテスト")
class CustomerEditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        // テストデータのセットアップ
        testCustomer = new Customer(...);
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testCustomer));
    }

    @Test
    @DisplayName("顧客情報を更新できる")
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void updateCustomer() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "John Doe");
        params.add("email", "john@example.com");
        
        mockMvc.perform(post("/mypage/edit")
                .params(params)
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mypage/edit-complete"));

        verify(customerService, times(1)).updateCustomerInfo(any(Customer.class));
    }
}
```

**重要**:
- `@MockitoBean` でリポジトリ・サービスをモック化
- `@WithUserDetails` で認証ユーザーを設定
- `setupBefore = TestExecutionEvent.TEST_EXECUTION` を指定
- CSRF トークンは `.with(csrf())` で付与
- `@DisplayName` で日本語のテスト名を記述
- **リクエストパラメータは `MultiValueMap` と `.params()` を使用**（`.param()` の連鎖は避ける）

### 3. サービステスト

```java
@SpringBootTest
@DisplayName("CustomerService のテスト")
class CustomerServiceTest {

    @MockitoBean
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Test
    @DisplayName("顧客を登録できる")
    void registerCustomer() {
        // given
        Customer customer = new Customer(...);
        
        // when
        customerService.registerCustomer(customer);
        
        // then
        verify(customerRepository, times(1)).save(any(Customer.class));
    }
}
```

### 4. リポジトリテスト

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CustomerRepository のテスト")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("メールアドレスで顧客を検索できる")
    void findByEmail() {
        Optional<Customer> customer = customerRepository.findByEmail("test@example.com");
        assertThat(customer).isPresent();
    }
}
```

**重要**:
- テストクラス名はテスト対象のコントローラ/サービス名に対応させる
- 例: `MyPageController` → `MyPageControllerTest`
- 1つのコントローラに対して1つのテストクラス
- 重複したテストクラスは作成しない

### 5. テスト実施の必須事項

**新機能実装時の必須テスト**:
- リポジトリ層のテスト（`@MybatisTest`）
- サービス層のテスト（`@SpringBootTest`）
- コントローラ層のテスト（`@WebMvcTest` または `@SpringBootTest` + `@AutoConfigureMockMvc`）

**重要**:
- 新しいメソッドを追加した際は、必ず対応するテストを追加する
- リポジトリとサービスのテストを忘れない
- テスト追加後は `mvn clean test` で全テストを実行し、合格を確認する
- カバレッジ目標: ビジネスロジック（Service、Controller）100%、リポジトリ層100%

## コードスタイル

### 1. インポート
- ワイルドカードインポート（`import ....*;`）は使用しない
- 不要なインポートは削除する
- 使用していないアノテーションのインポートも削除
- IDE のコードフォーマッターを使用

### 2. 命名規則

#### Javaクラス・ファイル
- **クラス名**: PascalCase
- **メソッド名**: camelCase
- **定数**: UPPER_SNAKE_CASE
- **パッケージ名**: lowercase

#### レイヤー別の命名規則

**コントローラ（Controller）**:
- クラス名: `{機能名}Controller`
- 例: `CustomerController`, `MyPageController`, `PasswordResetController`
- ファイル名: クラス名と同じ `.java`
- テストクラス: `{クラス名}Test` → `CustomerControllerTest`

**サービス（Service）**:
- クラス名: `{機能名}Service`
- 例: `CustomerService`, `EmailService`, `PasswordResetService`
- ファイル名: クラス名と同じ `.java`
- テストクラス: `{クラス名}Test` → `CustomerServiceTest`

**リポジトリ（Repository）**:
- クラス名: `{エンティティ名}Repository`
- 例: `CustomerRepository`, `PasswordResetTokenRepository`
- ファイル名: クラス名と同じ `.java`
- テストクラス: `{クラス名}Test` → `CustomerRepositoryTest`

**モデル（Model）**:
- エンティティ: `{エンティティ名}` → `Customer`, `PasswordResetToken`
- フォーム: `{機能名}Form` → `CustomerForm`, `CustomerEditForm`, `ChangePasswordForm`
- ファイル名: クラス名と同じ `.java`

**設定（Config）**:
- クラス名: `{機能名}Config`
- 例: `SecurityConfig`, `GreenMailConfig`
- ファイル名: クラス名と同じ `.java`

**バリデーション（Validation）**:
- アノテーション: `@{検証名}` → `@CurrentPassword`
- バリデータ: `{検証名}Validator` → `CurrentPasswordValidator`

**セキュリティ（Security）**:
- UserDetails実装: `{エンティティ名}UserDetails` → `CustomerUserDetails`
- UserDetailsService実装: `{エンティティ名}UserDetailsService` → `CustomerUserDetailsService`

#### HTMLテンプレート

**命名パターン**:
- 単一画面: `{機能名}.html` → `home.html`, `login.html`, `mypage.html`
- 一覧画面: `{エンティティ名}-list.html` → `customer-list.html`
- 入力画面: `{エンティティ名}-input.html` または `{機能名}-{操作}.html`
  - 例: `customer-input.html`, `customer-edit.html`, `change-password.html`
- 確認画面: `{機能名}-confirm.html` → `customer-confirm.html`, `customer-edit-confirm.html`
- 完了画面: `{機能名}-complete.html` → `customer-complete.html`, `change-password-complete.html`
- エラー画面: `{機能名}-error.html` → `customer-error.html`, `customer-registration-error.html`

**重要な原則**:
- **機能に基づく命名**: ファイル名は機能や業務を明確に表す
- **汎用的な名前は避ける**: `business-error.html`, `common-error.html` などの抽象的な名前は使用しない
- **既存ファイルの見直し**: 新規作成時に既存の類似ファイルがガイドラインに沿っているか確認する
- **一貫性の維持**: 同じパターンを繰り返し適用する

**リクエストとの対応**:
- リクエスト: `POST /customers/register` → 完了画面: `customer-complete.html`
- リクエスト: `POST /mypage/edit-confirm` → 確認画面: `customer-edit-confirm.html`
- ハイフン区切りで複数単語を表現

#### メソッド命名規則

**コントローラメソッド**:
- GET（画面表示）: `show{画面名}Page()` → `showMyPage()`, `showEditPage()`
- POST（処理実行）: `{動詞}{処理名}()` → `registerCustomer()`, `updateCustomer()`, `deleteCustomer()`
- POST（確認画面表示）: `show{機能名}ConfirmPage()` → `showEditConfirmPage()`
- POST（Backボタン）: `handleBackTo{画面名}()` → `handleBackToEdit()`

**サービスメソッド**:
- 取得: `get{対象}()` → `getAllCustomers()`
- 登録: `register{対象}()` → `registerCustomer()`
- 更新: `update{対象}()` または `change{対象}()` → `updateCustomerInfo()`, `changePassword()`
- 削除: `delete{対象}()` → `deleteCustomer()`
- 送信: `send{対象}()` → `sendResetLink()`

**リポジトリメソッド**:
- 全件取得: `findAll()`
- 条件検索: `findBy{条件}()` → `findByEmail()`, `findByToken()`
- 保存: `save({エンティティ})`
- 更新: `update{項目}()` → `updatePassword()`, `updateCustomerInfo()`
- 削除: `deleteBy{条件}()` → `deleteByEmail()`
- 定数: UPPER_SNAKE_CASE
- パッケージ名: lowercase

### 3. コメント
- JavaDoc は public メソッドに記述
- 複雑なロジックには適切なコメントを追加
- 日本語コメント可

## プロジェクト設定

### VSCode設定（.vscode/settings.json）

```json
{
  "java.configuration.updateBuildConfiguration": "interactive",
  "java.dependency.packagePresentation": "flat",
  "java.compile.nullAnalysis.mode": "disabled"
}
```

**重要**:
- `java.compile.nullAnalysis.mode` は `"disabled"` に設定
- Eclipse JDTのnull安全性チェックはSpring TestやHamcrestなどの外部ライブラリで多数の誤検知を生む
- null安全性は実行時テストとコードレビューで担保

## 禁止事項

1. **@ActiveProfiles の使用禁止**
   - `application.yml` と `@ConditionalOnProperty` で制御

2. **@Profile の使用を避ける**
   - `@ConditionalOnProperty` を優先

3. **XMLマッパーの使用禁止**
   - アノテーションベースのSQL定義を使用

4. **重複したテスト設定の禁止**
   - 共通設定は `application.yml` に集約

5. **不要なインポートを残さない**
   - コードレビュー前に必ず確認

6. **Thymeleafのリンクは必ずth:hrefを使用**
   - `th:href="@{/path}"` を使用（推奨）
   - `href="/path"` は禁止

## カバレッジ目標

- ビジネスロジック（Service、Controller）: 100%
- リポジトリ層: 100%
- 設定クラス: 除外可能
- main メソッド: 除外可能

## 参考資料

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MyBatis Documentation](https://mybatis.org/mybatis-3/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
