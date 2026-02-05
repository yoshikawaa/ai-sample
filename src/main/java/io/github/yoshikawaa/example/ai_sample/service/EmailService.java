package io.github.yoshikawaa.example.ai_sample.service;

import org.springframework.lang.NonNull;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * メール送信
     * 
     * @param to 送信先メールアドレス
     * @param subject 件名
     * @param body 本文
     * @return 送信成功時true、失敗時false
     */
    public boolean sendEmail(@NonNull String to, @NonNull String subject, @NonNull String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true を指定すると HTML メールを送信可能

            mailSender.send(message);
            return true;
        } catch (MailException | MessagingException e) {
            log.error("メール送信失敗: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }
}
