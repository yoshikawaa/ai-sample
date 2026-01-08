package io.github.yoshikawaa.example.ai_sample.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Customer {
    private String email;
    private String password;
    private String name;
    private LocalDate registrationDate;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
}
