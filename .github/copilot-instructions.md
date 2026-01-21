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
- **CSV**: OpenCSV 5.9

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
- **設計原則**:
  - **テストのためだけのメソッドは作らない**：本番コードで使用されないメソッドは実装しない
  - **ページネーション/ソート機能を優先**：大量データを想定し、全件取得メソッド（`findAll()`）よりもページネーション対応メソッド（`findAllWithPagination()`）やソート対応メソッド（`findAllWithSort()`）を優先的に使用
  - **実用的なメソッドのみ実装**：実際のユースケースに基づいてメソッドを設計

### サービス層（Service）
- **責務**: ビジネスロジック層。業務ルールの実装を担当
- **役割**:
  - ビジネスルールの実装（未成年チェック、パスワード暗号化等）
  - トランザクション管理（`@Transactional`）
  - 複数のリポジトリの組み合わせ
  - 認証情報の更新（`SecurityContextHolder`）
- **禁止事項**: HTTPリクエスト/レスポンスに依存しない
- **設計原則**:
  - **テストのためだけのメソッドは作らない**：コントローラから呼ばれないメソッドは実装しない
  - **単純なラッパーメソッドは避ける**：リポジトリをそのまま呼ぶだけのメソッド（例：`getAllCustomers()` → `customerRepository.findAll()`）は不要

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

#### 動的SQL
```java
@Select("""
    <script>
    SELECT * FROM customer
    <choose>
        <when test="sortColumn != null and sortColumn != ''">
            ORDER BY ${sortColumn} ${sortDirection}
        </when>
        <otherwise>
            ORDER BY registration_date DESC
        </otherwise>
    </choose>
    </script>
""")
List<Customer> findAllWithSort(@Param("sortColumn") String sortColumn, 
                                @Param("sortDirection") String sortDirection);
```

**重要**:
- 条件付きSQLには `<script>` タグを使用
- 相互排他的な条件（if-else）は `<choose>` + `<when>` + `<otherwise>` を使用（`<if>` の連続ではなく）
- 複数の独立した条件は `<if test>` を使用
- `${変数名}` は文字列置換（ソートカラム名など）、`#{変数名}` はプレースホルダ（値のバインド）
- null許容パラメータは `test="param != null and param != ''"` でチェック
- `<otherwise>` でデフォルト動作を明示
- **パフォーマンス重視**: 大量データの場合、Java側のソートではなくSQL側でソートを実装
  - メモリ使用量削減
  - データベースインデックスの活用
  - ネットワーク転送量の削減

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

// ✅ 推奨: 具体的なstaticインポート
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

// ❌ 禁止: staticワイルドカードインポート
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```

**重要**:
- ワイルドカードインポート（`import ....*;`）は使用しない
- **staticワイルドカードインポート**（`import static ....*;`）も使用しない
- 未使用のインポートは削除する
- IDEの自動インポート機能を活用
- 使用するメソッド・定数のみを明示的にインポート（コード可読性向上、IDE補完機能の活用）

#### FQCN（完全修飾クラス名）の使用禁止
```java
// ✅ 推奨: インポートして短縮形を使用
import org.springframework.data.domain.Sort;
import com.opencsv.bean.ColumnPositionMappingStrategy;

Sort.Order order = pageable.getSort().iterator().next();
ColumnPositionMappingStrategy<CustomerCsvDto> strategy = new ColumnPositionMappingStrategy<>();

// ❌ 禁止: FQCN（完全修飾クラス名）を直接使用
org.springframework.data.domain.Sort.Order order = pageable.getSort().iterator().next();
com.opencsv.bean.ColumnPositionMappingStrategy<CustomerCsvDto> strategy = 
    new com.opencsv.bean.ColumnPositionMappingStrategy<>();
```

**重要**:
- コード内でFQCNを直接使用しない（必ずimport文を使用）
- 可読性が向上し、コードが簡潔になる
- IDEのリファクタリング機能が正しく動作する

#### 文字列チェック
```java
// ✅ 推奨: StringUtils.hasText()を使用
import org.springframework.util.StringUtils;

if (StringUtils.hasText(name) || StringUtils.hasText(email)) {
    // 検索処理
}

// ❌ 非推奨: 手動でnullと空文字をチェック
if ((name != null && !name.isEmpty()) || (email != null && !email.isEmpty())) {
    // 検索処理
}
```

**重要**:
- `StringUtils.hasText()` を使用して文字列の有効性をチェック
- null、空文字（`""`）、空白文字のみ（`"   "`）を一度にチェック可能
- 冗長なnullチェックと空文字チェックを避ける
- 可読性が向上し、Spring Frameworkの標準パターンに準拠

#### コード簡潔化のベストプラクティス

##### 冗長な条件チェックの削減
```java
// ✅ 推奨: 必要最小限のチェック
if (sortInfo[0] != null) {
    // sortInfo[0]とsortInfo[1]は常に同時にnullまたは非nullになる
}

// ❌ 非推奨: 冗長なチェック
if (sortInfo[0] != null && sortInfo[1] != null) {
    // 両方チェックする必要がない
}
```

##### 不要な型変換の回避
```java
// ✅ 推奨: boolean値を直接使用
boolean ascending = order.getDirection().isAscending();
customers = sortCustomers(customers, property, ascending);

// ❌ 非推奨: boolean→String→booleanの往復変換
String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
boolean ascending = "ASC".equals(direction);
```

##### 不要な中間変数の削減
```java
// ✅ 推奨: メソッド呼び出しを直接引数に
customers = sortCustomers(customers, order.getProperty(), order.getDirection().isAscending());

// ❌ 非推奨: 一度しか使わない変数を宣言
String property = order.getProperty();
boolean ascending = order.getDirection().isAscending();
customers = sortCustomers(customers, property, ascending);
```

##### switchのdefaultケースの効率的な使用
```java
// ✅ 推奨: 同じ処理をdefaultで統合
String column = switch (property) {
    case "email" -> "email";
    case "name" -> "name";
    case "birthDate" -> "birth_date";
    default -> "registration_date";  // registrationDateと未知のプロパティの両方をカバー
};

// ❌ 非推奨: defaultと同じ処理を個別に記述
String column = switch (property) {
    case "email" -> "email";
    case "name" -> "name";
    case "registrationDate" -> "registration_date";  // defaultと同じ
    case "birthDate" -> "birth_date";
    default -> "registration_date";
};
```

**重要**:
- ロジックが保証する場合は冗長なチェックを省略
- 型変換の往復を避け、元の型のまま使用
- 一度しか使わない変数は宣言せず直接使用
- switchのdefaultで複数のケースをまとめる

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

#### メソッド配置順序
```java
@Service
public class CustomerService {
    
    // 1. フィールド
    private final CustomerRepository customerRepository;
    
    // 2. コンストラクタ
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    // 3. publicメソッド
    public List<Customer> getAllCustomers() { }
    public Customer getCustomerByEmail(String email) { }
    public void registerCustomer(Customer customer) { }
    
    // 4. privateメソッド
    private boolean isUnderage(LocalDate birthDate) { }
    private String[] extractSortInfo(Pageable pageable) { }
}
```

**重要**:
- **メソッドの配置順序**: publicメソッド → privateメソッド
- publicメソッドはクラスの公開インターフェースであり、先に配置することで可読性が向上
- privateメソッドは実装の詳細であり、後に配置

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

#### CSV出力用DTOパターン
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCsvDto {

    @CsvBindByName(column = "Email")
    @CsvBindByPosition(position = 0)
    private String email;

    @CsvBindByName(column = "Name")
    @CsvBindByPosition(position = 1)
    private String name;

    @CsvBindByName(column = "Registration Date")
    @CsvBindByPosition(position = 2)
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate registrationDate;

    @CsvBindByName(column = "Birth Date")
    @CsvBindByPosition(position = 3)
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate birthDate;

    @CsvBindByName(column = "Phone Number")
    @CsvBindByPosition(position = 4)
    private String phoneNumber;

    @CsvBindByName(column = "Address")
    @CsvBindByPosition(position = 5)
    private String address;

    /**
     * Customerエンティティから変換
     */
    public static CustomerCsvDto fromEntity(Customer customer) {
        return new CustomerCsvDto(
            customer.getEmail(),
            customer.getName(),
            customer.getRegistrationDate(),
            customer.getBirthDate(),
            customer.getPhoneNumber(),
            customer.getAddress()
        );
    }
}
```

**重要**:
- **DTOパターンを使用**: ドメイン層（Customer）をプレゼンテーション層（CSV）から分離
- **`@CsvBindByName`**: ヘッダー名を定義
- **`@CsvBindByPosition`**: カラムの順序を制御（必須）
- **`@CsvDate`**: 日付フォーマットを指定
- **`fromEntity()`**: エンティティからDTOへの変換メソッド
- CSV出力専用のクラスとして model パッケージに配置

**アーキテクチャ上の利点**:
- ドメインエンティティがCSV出力形式に依存しない
- 複数のCSVフォーマットを容易に追加可能
- 将来的に他の出力形式（Excel、JSON等）も同様のパターンで実装可能

#### CSV生成処理（Service層）
```java
private byte[] generateCSV(List<Customer> customers) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
        
        // UTF-8 BOMを追加（Excelでの文字化け防止）
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);
        
        // ヘッダー行を明示的に書き込み（@CsvBindByNameのカラム名を@CsvBindByPositionの順序で出力）
        osw.write("Email,Name,Registration Date,Birth Date,Phone Number,Address\n");
        osw.flush();
        
        // CustomerエンティティをCustomerCsvDtoに変換
        List<CustomerCsvDto> csvDtos = customers.stream()
            .map(CustomerCsvDto::fromEntity)
            .collect(Collectors.toList());
        
        // OpenCSVを使用してデータ行を生成（@CsvBindByPositionで順序制御）
        com.opencsv.bean.ColumnPositionMappingStrategy<CustomerCsvDto> strategy = 
            new com.opencsv.bean.ColumnPositionMappingStrategy<>();
        strategy.setType(CustomerCsvDto.class);
        
        StatefulBeanToCsv<CustomerCsvDto> beanToCsv = new StatefulBeanToCsvBuilder<CustomerCsvDto>(osw)
            .withMappingStrategy(strategy)
            .withApplyQuotesToAll(false)
            .build();
        
        beanToCsv.write(csvDtos);
        osw.flush();
        
        return baos.toByteArray();
    } catch (Exception e) {
        throw new RuntimeException("CSV生成中にエラーが発生しました", e);
    }
}
```

**重要**:
- **UTF-8 BOM**: Excelでの文字化け防止のため必須
- **ヘッダー手動書き込み**: `@CsvBindByName`のカラム名を`@CsvBindByPosition`の順序で出力
- **ColumnPositionMappingStrategy**: データ行の順序を`@CsvBindByPosition`で制御
- **ハイブリッドアプローチ**: ヘッダーは手動、データ行は自動生成
  - カスタムMappingStrategyによる完全自動化は実装が複雑でデータ行が正しく出力されない問題あり
  - 現在の方式がシンプルで確実
- **エスケープ処理**: OpenCSVが自動的に処理（ダブルクォート等）
- **`withApplyQuotesToAll(false)`**: 必要なフィールドのみをクォートで囲む

**禁止事項**:
- ❌ ドメインエンティティに直接CSV出力用アノテーションを付与しない
- ❌ カスタムMappingStrategyで完全自動化を試みない（データ行が出力されない）
- ❌ 手動でCSVエスケープ処理を実装しない（OpenCSVに任せる）

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

#### ページネーション

**ソート状態を保持する方法**:
```html
<!-- ✅ 推奨: プロパティとディレクションを個別に取得 -->
<a th:href="@{/customers(
    page=${customerPage.number + 1}, 
    size=${customerPage.size}, 
    sort=${customerPage.sort.isSorted() ? customerPage.sort.iterator().next().property + ',' + customerPage.sort.iterator().next().direction : null}
)}">Next</a>

<!-- ❌ 禁止: toString()を使用（ページ遷移でソート値が増殖する） -->
<a th:href="@{/customers(page=${customerPage.number + 1}, sort=${customerPage.sort.toString()})}">Next</a>
```

**重要**:
- `customerPage.sort.toString()` は使用禁止（ページ遷移ごとにソート値が重複して追加される）
- `isSorted()` でソートの有無を確認し、ソートがない場合は `null` を渡す
- `iterator().next().property` でソートプロパティ名を取得（例: `name`, `registrationDate`）
- `iterator().next().direction` でソート方向を取得（例: `ASC`, `DESC`）
- カンマ区切りで `property,direction` 形式にする（Spring MVCが `?sort=name,asc` を自動的にPageableにバインド）
- 検索パラメータがある場合は、それらも同様に保持する

**完全な例（検索＋ページネーション＋ソート）**:
```html
<a th:if="${customerPage.hasNext()}" 
   th:href="@{/customers(
       page=${customerPage.number + 1}, 
       size=${customerPage.size}, 
       name=${customerSearchForm.name}, 
       email=${customerSearchForm.email}, 
       sort=${customerPage.sort.isSorted() ? customerPage.sort.iterator().next().property + ',' + customerPage.sort.iterator().next().direction : null}
   )}"
   class="bg-gray-500 text-white px-3 py-1 rounded hover:bg-gray-600">
    Next &raquo;
</a>
```

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

#### Mockito ArgumentMatcher の使い分け

```java
// ✅ 推奨: any() - nullと非nullの両方にマッチ
when(customerRepository.findAllWithSort(any(), any())).thenReturn(...);

// ❌ 注意: anyString() - nullにはマッチしない（非nullの文字列のみ）
when(customerRepository.findAllWithSort(anyString(), anyString())).thenReturn(...);  // nullパラメータで失敗

// ✅ 推奨: eq() - 正確な値のマッチング
when(customerRepository.search(eq("John"), eq("john@example.com"))).thenReturn(...);

// ✅ 推奨: 混在可能
when(customerRepository.searchWithSort(eq("John"), any(), any(), any())).thenReturn(...);
```

**重要**:
- **`any()`**: null を含むすべての値にマッチ（null許容パラメータに使用）
- **`anyString()`**: 非nullの文字列のみにマッチ（nullが渡されると失敗）
- **`eq(value)`**: 正確な値のマッチング（特定の値を検証したい場合）
- **nullが渡される可能性のあるパラメータ**: 必ず `any()` を使用（`anyString()` は使用禁止）
- **複数のマッチャーを混在**: 一部を `eq()` で固定、残りを `any()` で柔軟にマッチ可能

#### モックデータの順序

```java
@Test
@DisplayName("getAllCustomersWithPagination: 登録日で降順ソートができる")
void testGetAllCustomersWithPagination_SortByRegistrationDateDesc() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("registrationDate").descending());
    
    // モックデータの順序はSQLの結果と一致させる（registration_date DESC）
    when(customerRepository.findAllWithPagination(10, 0, "registration_date", "DESC")).thenReturn(Arrays.asList(
        new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), ...),   // 2月（新しい）
        new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), ...) // 1月（古い）
    ));
    
    Page<Customer> result = customerService.getAllCustomersWithPagination(pageable);
    
    // 検証: モックデータと同じ順序で返される
    assertThat(result.getContent().get(0).getEmail()).isEqualTo("bob@example.com");
    assertThat(result.getContent().get(1).getEmail()).isEqualTo("alice@example.com");
}
```

**重要**:
- **モックデータの順序**: SQLクエリの結果と完全に一致させる
- **ソート順の考慮**: ASC/DESCに応じてテストデータを並べ替える
- **デフォルトソート**: ソート指定がない場合のデフォルト動作も考慮
- **検証**: 期待する順序でデータが返されることをアサート
- **誤った順序のモックデータ**: テストは成功してもバグが隠れる可能性がある
```

### 4. テストメソッド追加時の注意事項

**重要**:
- テストクラス名はテスト対象のコントローラ/サービス/リポジトリ名に対応させる
- 例: `MyPageController` → `MyPageControllerTest`
- 1つのクラスに対して1つのテストクラス
- 重複したテストクラスは作成しない
- **`@Test` には必ず `@DisplayName` を付けて日本語でテスト内容を記述する**
  - 例: `@DisplayName("顧客情報を更新できる")`
  - テストの目的が一目でわかるようにする
- **クラスの最後に新しいメソッドを追加する場合の重要なルール**
  - `oldString` には既存の最後のメソッドの閉じ括弧からクラスの閉じ括弧まで（`}\n}`）を含める
  - `newString` には既存の最後のメソッドの閉じ括弧 + 新しいメソッド全体 + クラスの閉じ括弧（`}\n\n    // 新メソッド...\n}`）を含める
  - これを忘れるとクラスの閉じ括弧が欠けてコンパイルエラーになる
  - 適用対象: すべてのJavaクラス（Service、Controller、Repository、Testなど）
- `@Test` アノテーションの重複に注意（コピー&ペーストミスを避ける）

### 5. リポジトリテスト

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

### 6. テスト実施の必須事項

**新機能実装時の必須テスト**:
- リポジトリ層のテスト（`@MybatisTest`）
- サービス層のテスト（`@SpringBootTest`）
- コントローラ層のテスト（`@WebMvcTest` または `@SpringBootTest` + `@AutoConfigureMockMvc`）

**重要**:
- 新しいメソッドを追加した際は、必ず対応するテストを追加する
- リポジトリとサービスのテストを忘れない
- テスト追加後は `mvn clean test` で全テストを実行し、合格を確認する
- カバレッジ目標: ビジネスロジック（Service、Controller）95-100%、リポジトリ層100%

**カバレッジが困難なエラーハンドリング**:
```java
private byte[] generateCSV(List<Customer> customers) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
        
        // CSV生成処理
        
        return baos.toByteArray();
    } catch (Exception e) {
        // NOTE: このcatchブロックは防御的プログラミングのために存在します。
        // ByteArrayOutputStreamとOpenCSVの通常動作では例外は発生しませんが、
        // 予期しないランタイムエラー（OutOfMemoryError等）からの保護として残しています。
        // テストでのカバレッジは困難ですが、本番環境での安全性のために必要です。
        throw new RuntimeException("CSV生成中にエラーが発生しました", e);
    }
}
```

**重要**:
- 実際には発生しないが防御的に残すエラーハンドリングには、その意図をコメントで明記する
- 「なぜテストできないのか」「なぜ残す必要があるのか」を説明する
- 将来のメンテナーに対して、誤って削除されないようにする
- カバレッジ100%を強制せず、95-98%を現実的な目標とする

**メソッド削除時の必須確認事項**:
- **全ての層でテストを確認する**：Repository層、Service層、Controller層の全てのテストファイルを確認
- **削除したメソッドを使用しているテストを全て削除**：
  - 例：`CustomerRepository.findAll()` を削除した場合
    - ✅ `CustomerServiceTest` で `findAll()` を使用しているテストを削除
    - ✅ `CustomerRepositoryTest` で `findAll()` をテストしているテストも削除（**忘れやすい**）
- **削除後は必ずテストを実行**：不要なテストが残っていないか確認
- **体系的なチェック**：削除対象メソッドを grep 検索し、全ての使用箇所（本番コード＋テストコード）を特定

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
