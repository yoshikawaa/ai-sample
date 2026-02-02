package io.github.yoshikawaa.example.ai_sample.service;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("CsvService のテスト")
class CsvServiceTest {

    @Autowired
    private CsvService csvService;

    @Test
    @DisplayName("generateCustomerCsv: 顧客リストをCSV形式にエクスポートできる")
    void testGenerateCustomerCsv() {
        // テストデータの準備
        List<Customer> customers = Arrays.asList(
            new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER),
            new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 1), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER)
        );

        // サービスメソッドを呼び出し
        byte[] csvData = csvService.generateCustomerCsv(customers);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // UTF-8 BOMを確認
        assertThat(csv).startsWith("\uFEFF");
        
        // ヘッダーを確認
        assertThat(csv).contains("Email,Name,Registration Date,Birth Date,Phone Number,Address");
        
        // データを確認
        assertThat(csv).contains("alice@example.com");
        assertThat(csv).contains("Alice");
        assertThat(csv).contains("1990-01-01");
        assertThat(csv).contains("bob@example.com");
        assertThat(csv).contains("Bob");
        assertThat(csv).contains("1992-02-02");
    }

    @Test
    @DisplayName("generateCustomerCsv: 空のリストをCSVエクスポートできる")
    void testGenerateCustomerCsv_EmptyList() {
        // 空のリスト
        List<Customer> customers = Collections.emptyList();

        // サービスメソッドを呼び出し
        byte[] csvData = csvService.generateCustomerCsv(customers);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // UTF-8 BOMとヘッダーのみ存在することを確認
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("Email,Name,Registration Date,Birth Date,Phone Number,Address");
        
        // データ行がないことを確認（ヘッダー行の後に改行のみ）
        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(1); // ヘッダーのみ
    }

    @Test
    @DisplayName("generateCustomerCsv: ダブルクォートを含むデータをエスケープできる")
    void testGenerateCustomerCsv_EscapeDoubleQuotes() {
        // ダブルクォートを含むテストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("test@example.com", "password", "Test \"Nickname\" User", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER)
        );

        // サービスメソッドを呼び出し
        byte[] csvData = csvService.generateCustomerCsv(customers);

        // 検証
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // OpenCSVがダブルクォートをエスケープすることを確認
        assertThat(csv).contains("\"Test \"\"Nickname\"\" User\"");
    }

    @Test
    @DisplayName("generateCustomerCsv: カンマを含むデータをクォートで囲める")
    void testGenerateCustomerCsv_EscapeCommas() {
        // カンマを含むテストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("test@example.com", "password", "User, Test", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "123 Main St, Apt 4", Customer.Role.USER)
        );

        // サービスメソッドを呼び出し
        byte[] csvData = csvService.generateCustomerCsv(customers);

        // 検証
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // OpenCSVがカンマを含むフィールドをクォートで囲むことを確認
        assertThat(csv).contains("\"User, Test\"");
        assertThat(csv).contains("\"123 Main St, Apt 4\"");
    }

    @Test
    @DisplayName("generateCustomerCsv: 改行を含むデータをエスケープできる")
    void testGenerateCustomerCsv_EscapeNewlines() {
        // 改行を含むテストデータ
        List<Customer> customers = Arrays.asList(
            new Customer("test@example.com", "password", "Test User", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "123 Main St\nApt 4", Customer.Role.USER)
        );

        // サービスメソッドを呼び出し
        byte[] csvData = csvService.generateCustomerCsv(customers);

        // 検証
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // OpenCSVが改行を含むフィールドをクォートで囲むことを確認
        assertThat(csv).contains("\"123 Main St\nApt 4\"");
    }

    @Test
    @DisplayName("generateCustomerCsv: 大量のデータをエクスポートできる")
    void testGenerateCustomerCsv_LargeDataset() {
        // 大量のテストデータを生成（1000件）
        List<Customer> customers = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            customers.add(new Customer(
                "user" + i + "@example.com",
                "password",
                "User " + i,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "111-1111",
                "Address" + i,
                Customer.Role.USER
            ));
        }

        // サービスメソッドを呼び出し
        byte[] csvData = csvService.generateCustomerCsv(customers);

        // 検証
        assertThat(csvData).isNotEmpty();
        String csv = new String(csvData, java.nio.charset.StandardCharsets.UTF_8);
        
        // ヘッダー行 + 1000データ行 = 1001行
        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(1001);
        
        // 最初と最後のユーザーを確認
        assertThat(csv).contains("user0@example.com");
        assertThat(csv).contains("user999@example.com");
    }
}
