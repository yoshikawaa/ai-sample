package io.github.yoshikawaa.example.ai_sample.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AdminCustomerEditForm {
    @NotBlank
    private String name;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String address;

    @NotNull
    private Customer.Role role;
}
