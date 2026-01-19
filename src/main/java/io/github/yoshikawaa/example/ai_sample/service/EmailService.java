package io.github.yoshikawaa.example.ai_sample.service;

import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(@NonNull String to, @NonNull String subject, @NonNull String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true を指定すると HTML メールを送信可能

            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            throw new IllegalStateException("メール送信中にエラーが発生しました", e);
        }
    }
}
