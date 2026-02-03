package io.github.yoshikawaa.example.ai_sample.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class LoginHistorySearchForm {
    private String email;
    private String status;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;
}
