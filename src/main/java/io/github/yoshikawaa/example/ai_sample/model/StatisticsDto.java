package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 統計データをまとめて保持するDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDto {
    private List<CustomerStatistics> customerStatistics;
    private List<LoginStatistics> loginStatistics;
    private List<UsageStatistics> usageStatistics;
    private LocalDate startDate;
    private LocalDate endDate;
}
