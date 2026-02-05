package io.github.yoshikawaa.example.ai_sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 通知履歴検索フォーム
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistorySearchForm {

    private String recipientEmail;
    private NotificationHistory.NotificationType notificationType;
    private NotificationHistory.Status status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
