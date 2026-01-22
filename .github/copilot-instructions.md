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

## 開発ワークフロー

### 1. 大規模変更時の事前計画

**対象となる変更**:
- 複数のファイル（5ファイル以上）に影響する変更
- 新しいパッケージやクラスの追加
- アーキテクチャレベルの変更（エラーハンドリング統一、認証機能追加等）
- 既存の機能の大幅なリファクタリング

**必須プロセス**:
1. **変更計画を提示**
2. **ユーザーの承認を待つ**
3. 承認後に実装開始

**計画に含める内容**:
```markdown
## 変更計画

### 目的
- 何を実現するか

### 影響範囲
- 新規作成: X個のファイル（パッケージ/クラス名をリスト）
- 修正: Y個のファイル（ファイル名をリスト）
- 削除: Z個のファイル（ファイル名をリスト）

### 実装アプローチ
1. ステップ1: 説明
2. ステップ2: 説明
3. ...

### リスクと注意点
- 潜在的な問題点
- 既存機能への影響

### テスト戦略
- どのようにテストするか
- 影響を受けるテストクラス
```

**重要**: 計画なしで大規模変更を開始しない。ユーザーが全体像を把握し、承認してから実装する。

### 2. コンパイルエラーチェックの徹底

**必須チェックポイント**:

✅ **ファイル編集直後**
- 特にインポート追加・削除時
- クラス名変更時
- メソッドシグネチャ変更時

✅ **multi_replace_string_in_file実行後**
- 複数ファイルを一括編集した場合は必須

✅ **テスト実行前**（最重要）
- `runTests`を実行する前に必ず`get_errors`でコンパイルエラーがないことを確認
- コンパイルエラーがある状態でテストを実行しない（時間の無駄）

**効率的なワークフロー**:
```
1. コード修正
2. get_errors でコンパイルエラーチェック ⬅️ 必須
3. エラーや警告があれば、その場で修正
   - コンパイルエラー（型の不一致、未実装メソッド等）
   - 未使用インポート警告
   - その他の警告
4. 再度 get_errors でチェック
5. エラー・警告なし確認後に runTests 実行 ⬅️ 1回のテスト実行で完了
```

**重要**: 
- ❌ 警告を無視してテスト実行 → テスト成功 → 警告修正 → 再テスト実行（非効率）
- ✅ 警告をその場で修正 → テスト実行 → 1回で完了（効率的）
- `runTests`を実行する前に必ず`get_errors`でコンパイルエラーがないことを確認
- コンパイルエラーがある状態でテストを実行しない（時間の無駄）

**禁止事項**:
- ❌ コンパイルエラーの確認なしにテスト実行
- ❌ エラーメッセージや警告を無視して次の作業に進む
- ❌ 「多分大丈夫」という前提で作業を進める

**良い例**:
```
1. 6個の例外クラス作成
2. get_errors でチェック → OK
3. GlobalExceptionHandler作成
4. get_errors でチェック → OK
5. 4個のサービス修正
6. get_errors でチェック → インポート不足を発見・修正
7. get_errors でチェック → OK
8. テスト修正
9. get_errors でチェック → OK
10. runTests 実行 → 全テスト成功
```

**悪い例**:
```
1. 複数ファイルを一括修正
2. runTests 実行 → コンパイルエラーで失敗（時間の無駄）
3. エラー修正
4. runTests 実行 → 別のコンパイルエラーで失敗
5. 繰り返し...
```

### 3. インポート文の管理

**ファイル編集後の必須確認**:
1. `get_errors`でコンパイルエラーをチェック
2. 未使用インポートの警告を確認
3. 不足しているインポートを追加
4. 未使用のインポートを削除

**特に注意が必要なケース**:
- メソッド削除後（使われなくなったインポートが残る）
- 例外クラス変更後（古い例外クラスのインポートが残る）
- テストメソッド削除後（アサーションメソッドのインポートが残る）

### 4. 段階的なコミット

**推奨**:
- 機能の一区切りごとにコミット
- コミット前に必ず全テストを実行
- コミットメッセージは明確に（例: `#51 add custom exception classes`）

**避けるべき**:
- 複数の機能を一度にコミット
- テスト失敗状態でのコミット

### 5. ガイドライン更新時のプロセス

**新しいルールをガイドラインに追加した場合、既存コードの整合性チェックが必須**：

**必須プロセス**:
1. **既存コード全体をレビュー**：grep検索等で新ルールに違反している箇所を特定
2. **違反箇所を修正**：新ルールに準拠するようにコードを更新
3. **テスト実行**：すべてのテストが通ることを確認
4. **コミット**：ガイドライン更新と既存コード修正を同じPRに含める

**具体例**:

```bash
# ログ出力ルールを追加した場合
# → 機密情報のログ出力を検索
grep -r "log.info.*token\|log.info.*password\|log.warn.*token" src/

# 命名規則を追加した場合
# → 既存のクラス名/メソッド名がルールに準拠しているか確認
find src/ -name "*.java" | xargs grep -l "class.*Service"

# 例外処理パターンを追加した場合
# → 既存の例外ハンドリングが新パターンに従っているか確認
grep -r "@ExceptionHandler" src/
```

**重要**:
- ❌ ガイドライン追加後に新規コードのみ適用し、既存コードを放置すると、不整合が発生する
- ✅ ガイドライン更新と同時に既存コード全体を新ルールに準拠させることで、一貫性を保つ
- ガイドラインは「理想の姿」ではなく「現在のコードベースが従うべきルール」である

**教訓**:
このプロセスを怠ると、ガイドラインで禁止したはずのパターン（例：機密情報のログ出力）が既存コードに残り続け、新規開発者や将来の自分が混乱する原因となる。

### 6. エラーハンドリングのアーキテクチャ

Spring Bootアプリケーションでは、**@ControllerAdvice**、**コントローラ内@ExceptionHandler**、**ErrorController**を適切に使い分ける。

#### 例外の分類

**ビジネス例外（BusinessException）**:
- ユーザーの入力やビジネスルールに関連
- HTTPステータス: 400（Bad Request）、404（Not Found）
- ユーザーに具体的な対処方法を提示可能
- 例: UnderageCustomerException、InvalidTokenException、CustomerNotFoundException

**システムエラー（RuntimeException）**:
- 技術的な問題（インフラ、外部システム連携）
- HTTPステータス: 500（Internal Server Error）
- ユーザーには「システムエラーが発生しました」と表示
- 例: EmailSendException、CsvGenerationException

**判断基準**:
```java
// ✅ ビジネス例外: ユーザーの操作で回避可能
public class UnderageCustomerException extends BusinessException {
    // 18歳未満は登録できない → ユーザーに「18歳以上が必要」と伝える
}

// ✅ システムエラー: 技術的な問題
public class EmailSendException extends RuntimeException {
    // メールサーバーとの通信失敗 → ユーザーには回避不可、システム管理者が対応
}
```

#### コントローラ内@ExceptionHandlerの使用

**対象**:
- **特定のコントローラでしか発生しない例外**
- コントローラ固有のビジネスロジック例外

**例**:
```java
@Controller
@RequestMapping("/register")
public class CustomerRegistrationController {
    
    /**
     * 未成年の顧客を登録しようとした場合のハンドラー
     * 顧客登録エラー画面を表示し、入力画面に戻れるようにする
     */
    @ExceptionHandler(UnderageCustomerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUnderageCustomerException(UnderageCustomerException ex, Model model) {
        logger.warn("Underage customer registration attempt: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "400");
        return "customer-registration-error";  // 専用エラー画面
    }
}
```

**重要**:
- コントローラ固有の例外は@ControllerAdviceではなく、コントローラ内で処理
- 専用のビジネスエラー画面を用意し、適切な戻り先（入力画面等）を提供
- UXを考慮: 確認画面→エラー→確認画面という複雑なフローを避ける

#### @ControllerAdvice（GlobalExceptionHandler）の使用

**対象**:
- **複数のコントローラで共通するビジネス例外**
- アプリケーション全体で統一的に処理すべき例外

**例**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCustomerNotFoundException(CustomerNotFoundException ex, Model model) {
        logger.warn("Customer not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", "404");
        return "error";  // 汎用エラー画面
    }
}
```

**重要**:
- コントローラ固有の例外は含めない
- 複数の機能で発生する可能性のある例外のみを処理

**セキュリティ上の注意**:
- **Enumeration Attack（列挙攻撃）に注意**：ユーザーの存在確認を許す例外処理は避ける
- 例：パスワードリセット、ユーザー登録などの認証・登録機能
- **悪い例**：
  ```java
  // ❌ セキュリティリスク: メールアドレスの存在有無が外部に漏れる
  public void sendResetLink(String email) {
      customerRepository.findByEmail(email)
          .orElseThrow(() -> new CustomerNotFoundException(email));  // 404エラー
      // トークン生成・メール送信
  }
  ```
- **良い例**：
  ```java
  // ✅ セキュリティ対策: 存在しないメールでも成功と同じ動作
  public void sendResetLink(String email) {
      var customerOpt = customerRepository.findByEmail(email);
      if (customerOpt.isEmpty()) {
          log.warn("パスワードリセット試行: 存在しないメールアドレス {}", email);
          return;  // 成功と同じ動作（例外をスローしない）
      }
      // トークン生成・メール送信
  }
  ```
- **原則**：認証・登録機能では、ユーザーの存在有無を外部に漏らさない（タイミング攻撃も考慮）

#### ErrorController の使用

**対象**:
- HTTPステータスエラー（404、500等）
- Spring MVCがキャッチできない低レベルエラー
- システムエラー（RuntimeExceptionをキャッチされずに到達）
- 詳細なエラーログが必要な場合

**実装方法**:
- `AbstractErrorController`を拡張（推奨）
- エラー属性の取得ユーティリティを活用
- ステータスコードに応じたテンプレート選択
- **ログレベルの使い分け**: 404→INFO、5xx→ERROR、その他→WARN

**例**:
```java
@Slf4j
@Controller
public class CustomErrorController extends AbstractErrorController {
    
    public CustomErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        HttpStatus status = getStatus(request);
        Map<String, Object> errorAttributes = getErrorAttributes(request, 
            ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, 
                                    ErrorAttributeOptions.Include.EXCEPTION));
        
        String path = (String) errorAttributes.get("path");
        String message = (String) errorAttributes.get("message");
        String exceptionType = (String) errorAttributes.get("exception");
        
        // ログレベルの使い分け
        if (status.is5xxServerError()) {
            // システムエラー: ERROR
            log.error("Server error occurred: status={}, path={}, message={}, exception={}", 
                status.value(), path, message, exceptionType);
        } else if (status == HttpStatus.NOT_FOUND) {
            // 404エラー: INFO（favicon等の正常な動作とユーザーの誤操作の両方を記録）
            log.info("Resource not found: path={}", path);
        } else {
            // その他のクライアントエラー: WARN
            log.warn("Client error: status={}, path={}, message={}", 
                status.value(), path, message);
        }
        
        model.addAllAttributes(errorAttributes);
        
        // ステータスコードに応じたテンプレート選択
        if (status == HttpStatus.NOT_FOUND) {
            return "error/404";
        } else if (status.is5xxServerError()) {
            return "error/500";
        }
        return "error/error";
    }
}
```

**重要**:
- **404エラーはINFOレベル**: ブラウザのfaviconリクエスト（正常）とユーザーの誤操作（存在しないURL）の両方が発生。path情報を含めることで後から分析可能
- **5xxエラーはERRORレベル**: サーバー側の問題は即座に対応が必要
- **その他のクライアントエラー（400番台）はWARNレベル**: ユーザーの不正な操作や入力

#### 使い分けの原則

| エラー種類 | 処理方法 | 遷移先 | 理由 |
|-----------|---------|--------|------|
| コントローラ固有のビジネス例外 | コントローラ内@ExceptionHandler | 専用ビジネスエラー画面 | UX重視、適切な戻り先を提供 |
| 共通のビジネス例外 | @ControllerAdvice | 汎用エラー画面 | 複数機能で共通処理 |
| システムエラー（RuntimeException） | ErrorController | error/500.html | 技術的問題、詳細ログ |
| HTTPステータスエラー（404等） | ErrorController | error/404.html | HTTPレベルのエラー |

**具体例**:
```
UnderageCustomerException（顧客登録時のみ）
  → CustomerRegistrationController@ExceptionHandler
  → customer-registration-error.html
  → /register/input（入力画面に戻る）

InvalidTokenException（パスワードリセット時のみ）
  → PasswordResetController@ExceptionHandler
  → password-reset-error.html
  → /password-reset/request（リクエスト画面に戻る）

CustomerNotFoundException（複数機能で発生）
  → @ControllerAdvice
  → error.html（汎用エラー画面）

EmailSendException（システムエラー）
  → ErrorController
  → error/500.html（システムエラー画面）

CsvGenerationException（システムエラー）
  → ErrorController
  → error/500.html（システムエラー画面）
```

#### 例外クラスの設計

**ビジネス例外**:
```java
// 基底クラス
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

// 派生クラス
public class UnderageCustomerException extends BusinessException {
    public UnderageCustomerException() {
        super("18歳未満のお客様は登録できません。");
    }
}
```

**システムエラー**:
```java
// RuntimeExceptionを直接継承
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**重要**:
- ビジネス例外はBusinessExceptionを継承
- システムエラーはRuntimeExceptionを直接継承
- BusinessExceptionをシステムエラーの基底クラスとして使用しない

#### 禁止事項

- ❌ `spring.mvc.throw-exception-if-no-handler-found` の使用（Spring Boot 3.0で非推奨）
- ❌ `spring.web.resources.add-mappings: false` の使用（静的リソースが無効化される）
- ❌ HTTPステータスエラーを@ControllerAdviceで処理（責務の混在）
- ❌ BasicErrorControllerの拡張（過剰に複雑）
- ❌ コントローラ固有の例外を@ControllerAdviceで処理（責務の分散）
- ❌ システムエラー（500）をBusinessExceptionから継承（例外分類の混在）
- ❌ コントローラで例外をキャッチして再スロー（不要なtry-catch）
- ❌ 広範な`catch (Exception e)`ブロック（適切な層に任せる）

**コントローラでの例外処理の原則**:
```java
// ❌ 禁止: 不要なtry-catch
@PostMapping("/request")
public String handleResetRequest(@RequestParam String email) {
    try {
        passwordResetService.sendResetLink(email);
        return "password-reset-request";
    } catch (Exception e) {
        // この例外は適切な層で処理すべき
        throw e;  // 単に再スローするだけなら不要
    }
}

// ✅ 推奨: 例外を適切な層に任せる
@PostMapping("/request")
public String handleResetRequest(@RequestParam String email) {
    passwordResetService.sendResetLink(email);
    return "password-reset-request";
}
```

**重要**:
- コントローラは例外をキャッチせず、適切な層（@ExceptionHandler、@ControllerAdvice、ErrorController）に任せる
- `catch (Exception e)`で再スローするだけの無駄なコードは避ける
- 例外処理が必要な場合は、コントローラ内の@ExceptionHandlerメソッドとして実装する

#### ビジネスエラー画面の設計

**原則**:
- ビジネス例外には専用のエラー画面を用意
- 汎用エラー画面（error.html）ではなく、機能固有のエラー画面を使用
- 適切な戻り先を提供（入力画面、リクエスト画面等）
- エラーメッセージは動的に表示（`th:text="${errorMessage}"`）

**例**:
```html
<!-- customer-registration-error.html -->
<div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6" 
     role="alert" th:text="${errorMessage}">
    エラーメッセージ
</div>
<a th:href="@{/register/input}" class="bg-blue-500 text-white px-4 py-2 rounded">
    Back to Registration
</a>
```

#### Thymeleafエラーテンプレート

**規約ベースのテンプレート配置**:
```
src/main/resources/templates/
  ├── customer-registration-error.html  # ビジネスエラー（顧客登録）
  ├── password-reset-error.html         # ビジネスエラー（パスワードリセット）
  ├── error.html                        # 汎用エラー（共通ビジネス例外）
  └── error/
      ├── 404.html                      # 404エラー専用
      ├── 500.html                      # 500エラー専用（システムエラー）
      ├── 4xx.html                      # 400番台のフォールバック
      └── 5xx.html                      # 500番台のフォールバック
```

**重要**:
- ビジネスエラー画面: templates/直下に配置（機能名-error.html）
- システムエラー画面: templates/error/配置（Spring Boot規約）
- Spring Bootは自動的にHTTPステータスコードに応じてerror/配下のテンプレートを選択

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

### 2. ログ出力

#### ロガーの実装方法

**必須**: すべてのクラスで `@Slf4j` アノテーションを使用する

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomerService {
    
    public void registerCustomer(Customer customer) {
        log.info("顧客登録開始: email={}", customer.getEmail());
        // ビジネスロジック
        customerRepository.save(customer);
        log.info("顧客登録完了: email={}", customer.getEmail());
    }
}
```

**禁止**: `Logger` の手動宣言は使用しない

```java
// ❌ 禁止: 手動でLoggerを宣言
private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

// ✅ 推奨: @Slf4jを使用
@Slf4j
public class CustomerService { ... }
```

#### ログレベルの使い分け

**ERROR**: システムエラー、予期しない例外
```java
try {
    emailService.sendEmail(to, subject, body);
} catch (Exception e) {
    log.error("メール送信失敗: to={}, subject={}", to, subject, e);
    throw new EmailSendException("メール送信に失敗しました", e);
}
```

**WARN**: ビジネス例外、セキュリティイベント、異常な操作
```java
// セキュリティイベント（列挙攻撃試行）
log.warn("パスワードリセット試行: 存在しないメールアドレス {}", email);

// ビジネス例外
log.warn("未成年の顧客登録試行: email={}, birthDate={}", email, birthDate);

// 無効なトークン
log.warn("無効なパスワードリセットトークン: token={}", token);
```

**INFO**: 重要な業務処理の開始・完了、状態変更
```java
// 顧客登録
log.info("顧客登録開始: email={}", customer.getEmail());
log.info("顧客登録完了: email={}", customer.getEmail());

// 顧客情報更新
log.info("顧客情報更新: email={}, name={}", customer.getEmail(), customer.getName());

// パスワード変更
log.info("パスワード変更完了: email={}", email);

// ログイン成功
log.info("ログイン成功: email={}", email);

// ログアウト
log.info("ログアウト: email={}", email);

// CSV エクスポート
log.info("CSV エクスポート実行: 件数={}, ユーザー={}", count, userEmail);
```

**DEBUG**: 開発・デバッグ用の詳細情報
```java
log.debug("検索条件: name={}, email={}, page={}, size={}", name, email, page, size);
log.debug("SQL実行: sortColumn={}, sortDirection={}", sortColumn, sortDirection);
```

#### ログ出力が必要な操作

**セキュリティ（必須）**:
- ✅ ログイン成功（AuthenticationSuccessHandler）
- ✅ ログイン失敗（AuthenticationFailureHandler）
- ✅ ログアウト（LogoutSuccessHandler）
- ✅ アクセス拒否（AccessDeniedHandler）
- ✅ セッションタイムアウト

**サービス層（必須）**:
- ✅ 顧客登録（registerCustomer）
- ✅ 顧客情報更新（updateCustomerInfo）
- ✅ 顧客削除（deleteCustomer）
- ✅ パスワード変更（changePassword）
- ✅ パスワードリセット（sendResetLink, updatePassword）
- ✅ CSVエクスポート（exportCustomersToCSV）

**コントローラ層（例外ハンドラのみ）**:
- ✅ @ExceptionHandlerメソッド内でのログ記録
- ❌ 通常のリクエストハンドラではログ不要（サービス層に任せる）

**エラーハンドラ**:
- ✅ GlobalExceptionHandler（@ControllerAdvice）
- ✅ CustomErrorController（ErrorController）
- ✅ コントローラ内の@ExceptionHandler

#### ログメッセージのフォーマット

**推奨形式**:
```java
// ✅ 推奨: 操作 + キー情報 + プレースホルダー
log.info("顧客登録開始: email={}", customer.getEmail());
log.info("顧客情報更新: email={}, name={}", customer.getEmail(), customer.getName());
log.warn("未成年の顧客登録試行: email={}, age={}", email, age);
log.error("CSV生成失敗: 件数={}", customers.size(), exception);
```

**禁止事項**:
```java
// ❌ 禁止: 文字列連結（パフォーマンス低下、例外情報の欠落）
log.info("顧客登録開始: email=" + customer.getEmail());

// ❌ 禁止: プレースホルダーなし（構造化ログが困難）
log.info("Customer registered: " + customer.getEmail());

// ❌ 禁止: 詳細すぎる情報（パスワード等の機密情報）
log.info("顧客登録: email={}, password={}", email, password);  // パスワードは絶対にログに出力しない
```

#### 機密情報の取り扱い

**絶対にログに出力してはいけない情報**:
- ❌ パスワード（平文、ハッシュ化後も含む）
- ❌ クレジットカード番号
- ❌ セキュリティトークン（パスワードリセットトークン等）の値
- ❌ 個人を特定できる詳細情報（住所、電話番号の全桁）

**ログに出力可能な情報**:
- ✅ メールアドレス（ビジネスキー）
- ✅ 顧客名
- ✅ 生年月日
- ✅ 操作の成功・失敗
- ✅ トークンの存在有無（値ではなく）

**例**:
```java
// ✅ 推奨
log.info("パスワード変更完了: email={}", email);
log.warn("無効なトークン: トークンが期限切れです");

// ❌ 禁止
log.info("パスワード変更: email={}, newPassword={}", email, newPassword);
log.warn("無効なトークン: token={}", token);  // トークンの値を出力しない
```

**例外: 開発環境でのデバッグ**:
開発環境でのデバッグ目的の場合、**DEBUGレベル**でトークン値を出力可能：

```java
// ✅ 許容される（開発環境のみ）
public void sendResetLink(String email) {
    // ... トークン生成 ...
    
    log.info("パスワードリセットリンク送信: email={}", email);  // 本番でも出力
    log.debug("リセットリンク：{}", resetLink);  // 開発環境のみ出力
}
```

**条件**:
- 本番環境ではDEBUGログが無効化されることを前提
- コメントで開発用であることを明記
- INFOレベルでは操作の事実のみを記録（トークン値は含めない）

#### テストコードでのログ出力

テストクラスにはログ出力を追加しない。プロダクションコードのログで十分。

```java
// ❌ テストクラスに@Slf4jは不要
@Slf4j  // 削除
@SpringBootTest
class CustomerServiceTest { ... }

// ✅ テストクラスではログ出力しない
@SpringBootTest
class CustomerServiceTest { ... }
```

#### ログ出力のテスト

**ログ出力はテストでアサートしない**。理由：

1. **テストの脆弱性**: ログメッセージの文言変更でテストが壊れる
2. **保守コストの増加**: ログフォーマット変更の度にテストを修正
3. **本質的な振る舞いではない**: ログは副作用であり、ビジネスロジックの本質ではない
4. **テストの複雑化**: ログキャプチャ（LogCaptor、@Captor等）の設定が複雑になる

```java
// ❌ 禁止: ログ出力をアサート
@Test
void testRegisterCustomer() {
    customerService.registerCustomer(customer);
    
    // ログキャプチャしてアサート（不要）
    verify(logger).info("顧客登録開始: email={}", customer.getEmail());
}

// ✅ 推奨: ビジネスロジックの結果のみアサート
@Test
void testRegisterCustomer() {
    customerService.registerCustomer(customer);
    
    // ビジネスロジックの結果を検証
    verify(customerRepository).save(any(Customer.class));
}
```

**例外**: 以下のケースではログ出力が重要な要件となるため、アサート可能
- 監査ログ（法的要件）
- セキュリティログ（インシデント対応）
- ただし、これらは専用の監査システムで管理すべきで、通常のログ出力とは別扱い

### 3. リポジトリ層（MyBatis）

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

#### メソッドの配置順序

リポジトリのメソッドは**機能ごとにグループ化**し、関連するメソッドを近接配置する：

```java
@Mapper
public interface CustomerRepository {
    // ========================================
    // 全件取得系
    // ========================================
    
    List<Customer> findAllWithSort(...);
    List<Customer> findAllWithPagination(...);
    long count();

    // ========================================
    // 検索系
    // ========================================

    List<Customer> searchWithSort(...);
    List<Customer> searchWithPagination(...);
    long countBySearch(...);

    // ========================================
    // 単一取得
    // ========================================

    Optional<Customer> findByEmail(String email);

    // ========================================
    // 登録
    // ========================================

    void save(Customer customer);

    // ========================================
    // 更新
    // ========================================

    void updatePassword(...);
    void updateCustomerInfo(...);

    // ========================================
    // 削除
    // ========================================

    void deleteByEmail(String email);
}
```

**重要**:
- **機能ごとにセクションコメントで区切る**: 全件取得、検索、単一取得、登録、更新、削除
- **関連メソッドを近接配置**: `findAllXxx`系、`searchXxx`系をそれぞれグループ化
- **一貫性のあるパターン**: 各グループで「ソート → ページネーション → カウント」の順序
- **CRUD操作の順序**: 取得（Read）→ 登録（Create）→ 更新（Update）→ 削除（Delete）
- **可読性とメンテナンス性**: 関連するメソッドが離れていると理解しづらい

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

**コード作成・編集後の確認プロセス**:
1. **ファイル編集後**: 必ずコンパイルエラーと未使用インポートをチェック
2. **大規模変更後**（multi_replace_string_in_file使用時など）:
   - 変更したすべてのファイルでインポートを確認
   - 削除されたメソッド呼び出しに関連するインポートを削除
   - 追加されたメソッド呼び出しに必要なインポートを追加
3. **テスト実行前**: コンパイルエラーがないことを確認
4. **コミット前**: 最終チェックとして全ファイルの未使用インポートを確認

**未使用インポートのチェック方法**:
- IDEのコード検査機能を使用（グレーアウトされたインポートを確認）
- `get_errors`ツールでコンパイルエラーと警告を確認
- 特にテストクラスでは、使われなくなったアサーションメソッド（`assertThrows`等）のインポートに注意

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

**すべてのクラスに共通する配置順序**:
1. フィールド
2. コンストラクタ
3. publicメソッド（クラスの公開インターフェース）
4. privateメソッド（実装の詳細）

**重要**:
- publicメソッドを先に配置することで可読性が向上
- privateメソッドは実装の詳細であり、後に配置

**サービス・リポジトリクラスの例**:
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

**コントローラクラスの追加ルール**:
```java
@Controller
@RequestMapping("/customers")
public class CustomerController {
    
    // 1. フィールド
    private final CustomerService customerService;
    
    // 2. コンストラクタ
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    // 3. リクエストハンドラメソッド
    @GetMapping
    public String showCustomers() { }
    
    @PostMapping
    public String registerCustomer() { }
    
    // 4. @ExceptionHandlerメソッド（最後に配置）
    @ExceptionHandler(InvalidTokenException.class)
    public String handleInvalidTokenException() { }
}
```

**重要（コントローラ固有）**:
- **publicメソッドの配置順序**: リクエストハンドラメソッド → @ExceptionHandlerメソッド
- @ExceptionHandlerは例外処理の詳細であり、最後に配置

**設定クラスの追加ルール**:
```java
@Configuration
public class SecurityConfig {
    
    // 1. フィールド（ある場合）
    
    // 2. メイン設定Bean（クラスの中心的な責務）
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) { }
    
    // 3. サポートBean（メイン設定をサポートする実装の詳細）
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() { }
    
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() { }
    
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() { }
}
```

**重要（設定クラス固有）**:
- **メイン設定Beanを最初に配置**: クラスの中心的な責務（例: SecurityFilterChain）
- **サポートBeanは後に配置**: メイン設定で使用されるハンドラーやヘルパーBean
- **可読性の向上**: 「何を設定しているか」→「どう実装しているか」の順序

#### ロジックの共通化と重複排除

同じ検証・処理ロジックを複数のメソッドで実装している場合は、privateメソッドに抽出して共通化する。**この原則はすべてのレイヤー（サービス層、コントローラ層、リポジトリ層など）に適用される**：

```java
@Service
public class PasswordResetService {
    
    // ✅ 推奨: 共通のロジックをprivateメソッドに抽出
    public void validateResetToken(String token) {
        getValidatedToken(token);
    }

    public void updatePassword(String token, String newPassword) {
        PasswordResetToken resetToken = getValidatedToken(token);
        String email = resetToken.getEmail();
        
        String hashedPassword = passwordEncoder.encode(newPassword);
        customerRepository.updatePassword(email, hashedPassword);
        passwordResetTokenRepository.deleteByEmail(email);
    }

    /**
     * トークンを検証し、有効なPasswordResetTokenを返す
     */
    private PasswordResetToken getValidatedToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByResetToken(token);
        if (resetToken == null || resetToken.getTokenExpiry() < System.currentTimeMillis()) {
            throw new InvalidTokenException();
        }
        return resetToken;
    }
}
```

```java
// ❌ 非推奨: 同じロジックを複数のメソッドで重複実装
public void validateResetToken(String token) {
    PasswordResetToken resetToken = passwordResetTokenRepository.findByResetToken(token);
    if (resetToken == null || resetToken.getTokenExpiry() < System.currentTimeMillis()) {
        throw new InvalidTokenException();
    }
}

public void updatePassword(String token, String newPassword) {
    // 同じ検証ロジックを再実装（DRY原則違反）
    PasswordResetToken resetToken = passwordResetTokenRepository.findByResetToken(token);
    if (resetToken == null || resetToken.getTokenExpiry() < System.currentTimeMillis()) {
        throw new InvalidTokenException();
    }
    // パスワード更新処理
}
```

**重要**:
- **DRY原則（Don't Repeat Yourself）**: 同じロジックを複数箇所に書かない
- **保守性**: ロジック変更時に1箇所を修正すれば全体に反映される
- **効率性**: DB呼び出しなどを1回にまとめられる
- **メソッド呼び出しの最適化**: 同じメソッド呼び出し（例: `resetToken.getEmail()`）を複数回実行する場合は、変数に格納して再利用する
- **適用範囲**: サービス層、コントローラ層、リポジトリ層など、すべてのレイヤーで適用

### 4. サービス層

#### 汎用的なサービス設計（ジェネリクスの活用）

特定のエンティティに依存しない処理は、ジェネリクスを使用して汎用化する：

```java
@Service
public class CsvService {
    
    /**
     * 汎用的なCSV生成メソッド（任意のDTOクラスに対応）
     */
    public <T> byte[] generateCsv(List<T> dtos, Class<T> dtoClass, String header) {
        // @CsvBindByPosition を使用した汎用的なCSV生成
    }
    
    /**
     * Customer特化のラッパーメソッド（後方互換性と利便性のため）
     */
    public byte[] generateCustomerCsv(List<Customer> customers) {
        List<CustomerCsvDto> csvDtos = customers.stream()
            .map(CustomerCsvDto::fromEntity)
            .collect(Collectors.toList());
        String header = "Email,Name,Registration Date,Birth Date,Phone Number,Address\n";
        return generateCsv(csvDtos, CustomerCsvDto.class, header);
    }
}
```

**重要**:
- **ジェネリクスで汎用化**: 特定のエンティティに依存しない設計
- **ラッパーメソッドで利便性**: エンティティ特化のメソッドも残す
- **将来の拡張性**: `Order`, `Product` などの他のエンティティにも対応可能
- **単一責任**: CSV処理に特化したサービスとして分離

**利点**:
- コードの重複を防ぐ
- 新しいエンティティのCSV対応が容易
- テストが書きやすい（モック化しやすい）

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

#### テスト用コントローラの定義
```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GlobalExceptionHandler のテスト")
class GlobalExceptionHandlerTest {
    
    @TestConfiguration
    static class TestConfig {
        
        @Controller
        @RequestMapping("/test")
        static class TestController {
            
            @Autowired
            private CustomerService customerService;
            
            @GetMapping("/customer-not-found")
            public String throwCustomerNotFoundException() {
                customerService.getCustomerByEmail("nonexistent@example.com");
                return "success";
            }
        }
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private CustomerService customerService;
    
    @Test
    @DisplayName("CustomerNotFoundException が発生した場合、error.html を表示する")
    void testCustomerNotFoundException() throws Exception {
        when(customerService.getCustomerByEmail("nonexistent@example.com"))
            .thenThrow(new CustomerNotFoundException("Customer not found"));
        
        mockMvc.perform(get("/test/customer-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error"));
    }
}
```

**重要**:
- **テスト用コントローラは `@TestConfiguration` 内に static nested class として定義**
- **独立したファイル（例: TestController.java）として作成しない**
- `@Controller` アノテーションを付与してSpringに認識させる
- テストしたいエンドポイントだけを定義
- サービスは `@Autowired` でインジェクション（テストクラスで `@MockitoBean` を使用）
- **利点**:
  - テストとテスト用コントローラの結合度が高く、意図が明確
  - テストファイルを開けば全ての関連コードが見える
  - 不要なファイルの散在を防ぐ
  - Spring Testing の推奨パターン

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

**テストメソッドの配置順序**:
- **リポジトリテスト**: リポジトリメソッドの定義順と一致させる
  - セクションコメント（`// ========================================`）で機能ごとにグループ化
  - 全件取得系 → 検索系 → 単一取得 → 登録 → 更新 → 削除
- **サービステスト**: サービスメソッドの実装順と一致させる
  - 各メソッドに対応するテストをまとめて配置
  - メソッドに複数のテストケースがある場合は近接配置
  - 例: `getCustomerByEmail()` → `getCustomerByEmail_NotFound()`
- **コントローラテスト**: コントローラメソッドの定義順と一致させる
  - GETメソッドのテスト → POSTメソッドのテスト

**例（リポジトリテスト）**:
```java
@MybatisTest
@DisplayName("CustomerRepository のテスト")
class CustomerRepositoryTest {
    // ========================================
    // 全件取得系
    // ========================================
    @Test
    @DisplayName("findAllWithPagination: ページネーションで顧客を取得できる")
    void testFindAllWithPagination() { }
    
    @Test
    @DisplayName("count: 全顧客数を取得できる")
    void testCount() { }
    
    // ========================================
    // 検索系
    // ========================================
    @Test
    @DisplayName("searchWithPagination: 検索条件でページネーション")
    void testSearchWithPagination() { }
}
```

**例（サービステスト）**:
```java
@SpringBootTest
@DisplayName("CustomerService のテスト")
class CustomerServiceTest {
    // ========================================
    // 単一取得
    // ========================================
    @Test
    @DisplayName("getCustomerByEmail: メールアドレスで顧客を取得できる")
    void testGetCustomerByEmail() { }
    
    @Test
    @DisplayName("getCustomerByEmail: 存在しないメールアドレスの場合、例外をスローする")
    void testGetCustomerByEmail_NotFound() { }
    
    // ========================================
    // 全件取得+ページネーション
    // ========================================
    @Test
    @DisplayName("getAllCustomersWithPagination: ページネーションで顧客を取得できる")
    void testGetAllCustomersWithPagination() { }
    
    @Test
    @DisplayName("getAllCustomersWithPagination: 名前で昇順ソートができる")
    void testGetAllCustomersWithPagination_SortByNameAsc() { }
}
```

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

### 6. 例外クラステスト

**すべての例外クラスにテストを作成する**：

```java
@DisplayName("BusinessException のテスト")
class BusinessExceptionTest {

    @Test
    @DisplayName("メッセージ付きコンストラクタで例外を生成できる")
    void testConstructorWithMessage() {
        // Given
        String message = "テストエラーメッセージ";

        // When
        BusinessException exception = new BusinessException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("メッセージと原因付きコンストラクタで例外を生成できる")
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "テストエラーメッセージ";
        Throwable cause = new RuntimeException("原因の例外");

        // When
        BusinessException exception = new BusinessException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
```

**重要**:
- すべてのコンストラクタをテストする
- メッセージが正しく設定されることを確認
- 原因（cause）が正しく設定されることを確認
- デフォルトメッセージが正しいことを確認
- `@SpringBootTest` は不要（純粋なPOJOテスト）
```

### 6. テスト実施の必須事項

**新機能実装時の必須テスト**:
- リポジトリ層のテスト（`@MybatisTest`）
- サービス層のテスト（`@SpringBootTest`）
- コントローラ層のテスト（`@WebMvcTest` または `@SpringBootTest` + `@AutoConfigureMockMvc`）
- 例外クラスのテスト（純粋なPOJOテスト）

**重要**:
- 新しいメソッドを追加した際は、必ず対応するテストを追加する
- リポジトリとサービスのテストを忘れない
- 例外クラスを追加した際も、必ずテストを作成する
- テスト追加後は `mvn clean test` で全テストを実行し、合格を確認する
- カバレッジ目標: ビジネスロジック（Service、Controller）95-100%、リポジトリ層100%、例外クラス100%

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

**実用的でない防御的プログラミングの削除**:
```java
// ❌ 禁止: 実際の運用で発生しない防御的コード
@Bean
public LogoutSuccessHandler logoutSuccessHandler() {
    return (request, response, authentication) -> {
        if (authentication != null) {  // 認証済みユーザーのみがログアウトするため、常に非null
            log.info("ログアウト: email={}", authentication.getName());
        }
        response.sendRedirect("/");
    };
}

// ✅ 推奨: 実用的な実装
@Bean
public LogoutSuccessHandler logoutSuccessHandler() {
    return (request, response, authentication) -> {
        log.info("ログアウト: email={}", authentication.getName());
        response.sendRedirect("/");
    };
}
```

**重要**:
- 実際の運用で発生しないケースに対する防御的なnullチェックは削除する
- フレームワークの標準的な動作（例: Spring Securityは認証済みユーザーのログアウトを想定）に準拠する
- 過剰な防御コードはコードの複雑性を増し、テストカバレッジを低下させる
- 必要な防御（OutOfMemoryError等の予期しないエラー）と不要な防御（正常フローでは発生しないnullチェック）を区別する

**メソッド削除時の必須確認事項**:
- **全ての層でテストを確認する**：Repository層、Service層、Controller層の全てのテストファイルを確認
- **削除したメソッドを使用しているテストを全て削除**：
  - 例：`CustomerRepository.findAll()` を削除した場合
    - ✅ `CustomerServiceTest` で `findAll()` を使用しているテストを削除
    - ✅ `CustomerRepositoryTest` で `findAll()` をテストしているテストも削除（**忘れやすい**）
- **削除後は必ずテストを実行**：不要なテストが残っていないか確認
- **体系的なチェック**：削除対象メソッドを grep 検索し、全ての使用箇所（本番コード＋テストコード）を特定

## コードスタイル

### 1. 定数の使用

既存の定数が提供されている場合は必ず使用し、マジックナンバーや文字列リテラルの直接実装を避ける。

```java
// ✅ 推奨: 定数を使用
request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
response.setContentType(MediaType.APPLICATION_JSON_VALUE);

// ❌ 禁止: マジックナンバーや文字列リテラル
request.setAttribute("jakarta.servlet.error.status_code", 404);
return ResponseEntity.status(404).build();
response.setContentType("application/json");
```

**重要**:
- Jakarta EE、Spring Framework、Java標準ライブラリが提供する定数を優先
- IDEの補完機能を活用して定数を発見
- 定数を使うことでタイポを防ぎ、リファクタリングが容易になる

### 2. インポート
- ワイルドカードインポート（`import ....*;`）は使用しない
- 不要なインポートは削除する
- 使用していないアノテーションのインポートも削除
- IDE のコードフォーマッターを使用

### 3. 命名規則

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
- 例: `CustomerService`, `EmailService`, `PasswordResetService`, `CsvService`
- ファイル名: クラス名と同じ `.java`
- テストクラス: `{クラス名}Test` → `CustomerServiceTest`
- **将来の拡張性を考慮**: 
  - ❌ `CsvExportService` → 輸出のみに限定される
  - ✅ `CsvService` → 将来的にインポート、検証機能なども追加可能
  - 方向性（Export/Import）や操作（Create/Read）を含めない汎用的な名前を推奨

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

**テストメソッド**:
- **必ず`test`プレフィックスで始める**: `test{テスト対象メソッド名}()` → `testGetAllCustomers()`, `testRegisterCustomer()`
- バリエーション: `test{テスト対象メソッド名}_{条件}()` → `testGetCustomerByEmail_NotFound()`, `testExportCustomersToCSV_WithSort()`
- **禁止**: `test`プレフィックスなしのメソッド名（例: `exportCustomersToCSV()`, `loadUserByUsername_Success()`）
- **理由**: 統一された命名規則により、テストメソッドを即座に識別可能

**その他**:
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
- **例外クラス**: 100%（すべてのコンストラクタをテスト）
- **ErrorController**: 100%（エラーハンドリングは必須）
- **ControllerAdvice**: 100%（例外ハンドリングは必須）
- 設定クラス（@Configuration）: 除外可能
- main メソッド: 除外可能

**重要な注意事項**:
- `config`パッケージにあっても、`@Controller`、`@RestController`、`@ControllerAdvice`が付いているクラスは**必ずテストを実装する**
- 「設定クラス」とは`@Configuration`アノテーション付きのクラスのみを指す
- ErrorControllerやControllerAdviceのテストを省略すると、実装ミスが本番環境で発覚するリスクがある

### ErrorController / ControllerAdvice のテスト方法

**通常のコントローラと異なり、MockMvc経由ではエラーハンドリングフローが正しく動作しません。**

```java
@SpringBootTest
@DisplayName("CustomErrorController のテスト")
class CustomErrorControllerTest {

    @Autowired
    private CustomErrorController customErrorController;

    @Test
    @DisplayName("404エラーの場合、error/404.html を表示する")
    void testNotFoundError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error/404");
        assertThat(model.getAttribute("status")).isEqualTo(404);
    }
}
```

**重要**:
- `MockHttpServletRequest`と`ExtendedModelMap`を使用（Springが提供するテストオブジェクト）
- Mockitoの過度な使用を避ける（@SpringBootTestの利点を活かす）
- MockMvc経由では404/500エラーが正しくErrorControllerに到達しない
- 定数の使用については「コードスタイル > 定数の使用」を参照

## 静的リソース

### Favicon

プロジェクトには必ずfaviconを追加する。faviconがないとブラウザが自動的に`/favicon.ico`をリクエストし、404エラーログが発生する。

**配置場所**:
- `src/main/resources/static/favicon.ico` （従来型、全ブラウザ対応）
- `src/main/resources/static/favicon.svg` （モダンブラウザ、SVG形式）

**作成方法**:
1. **既存の画像から変換**:
   - ImageMagickを使用: `magick convert input.png -resize 32x32 favicon.ico`
   - オンラインツール: [Favicon Generator](https://favicon.io/), [RealFaviconGenerator](https://realfavicongenerator.net/)

2. **シンプルなSVGアイコン**:
   ```xml
   <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
     <rect width="100" height="100" fill="#4F46E5"/>
     <text x="50" y="70" font-size="60" text-anchor="middle" fill="white" 
           font-family="Arial, sans-serif" font-weight="bold">C</text>
   </svg>
   ```

**重要**:
- faviconは初期プロジェクトセットアップ時に追加
- 404エラーログのノイズを防ぐ
- ブランディングとユーザー体験の向上

## 参考資料

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [MyBatis Documentation](https://mybatis.org/mybatis-3/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
