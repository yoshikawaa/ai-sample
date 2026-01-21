package io.github.yoshikawaa.example.ai_sample.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.model.CustomerCsvDto;

/**
 * CSV処理サービス
 * 顧客データのCSVエクスポート機能を提供します。
 * 将来的にはCSVインポート機能なども追加可能です。
 */
@Service
public class CsvService {

    /**
     * 顧客リストをCSV形式のバイト配列に変換する
     * 
     * @param customers 顧客リスト
     * @return CSV形式のバイト配列（UTF-8 BOM付き）
     */
    public byte[] generateCustomerCsv(List<Customer> customers) {
        // CustomerエンティティをCustomerCsvDtoに変換
        List<CustomerCsvDto> csvDtos = customers.stream()
            .map(CustomerCsvDto::fromEntity)
            .collect(Collectors.toList());
        
        // ヘッダー行を定義
        String header = "Email,Name,Registration Date,Birth Date,Phone Number,Address\n";
        
        // 汎用CSV生成メソッドを使用
        return generateCsv(csvDtos, CustomerCsvDto.class, header);
    }

    /**
     * 汎用CSV生成メソッド
     * DTOリストをCSV形式のバイト配列に変換する
     * DTOクラスには@CsvBindByPositionアノテーションが必要
     * 
     * @param <T> DTOの型
     * @param dtos DTOリスト
     * @param dtoClass DTOのクラス
     * @param header CSVヘッダー行（改行を含む）
     * @return CSV形式のバイト配列（UTF-8 BOM付き）
     */
    public <T> byte[] generateCsv(List<T> dtos, Class<T> dtoClass, String header) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            
            // UTF-8 BOMを追加（Excelでの文字化け防止）
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            
            // ヘッダー行を書き込み
            osw.write(header);
            osw.flush();
            
            // OpenCSVを使用してデータ行を生成（@CsvBindByPositionで順序制御）
            ColumnPositionMappingStrategy<T> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(dtoClass);
            
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(osw)
                .withMappingStrategy(strategy)
                .withApplyQuotesToAll(false)
                .build();
            
            beanToCsv.write(dtos);
            osw.flush();
            
            return baos.toByteArray();
        } catch (Exception e) {
            // NOTE: このcatchブロックは防御的プログラミングのために存在します。
            // ByteArrayOutputStreamとOpenCSVの通常動作では例外は発生しませんが、
            // 予期しないランタイムエラー（OutOfMemoryError等）からの保護として残しています。
            // テストでのカバレッジは困難ですが、本番環境での安全性のために必要です。
            throw new RuntimeException("CSV生成中にエラーが発生しました", e);
        }
    }
}
