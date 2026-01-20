package io.github.yoshikawaa.example.ai_sample.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * CSV出力用DTO
 * ドメイン層（Customer）をプレゼンテーション層の形式（CSV）から分離するために使用
 */
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
     * CustomerエンティティからCustomerCsvDtoへの変換
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
