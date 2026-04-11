package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendEmail(String to, String subject, String body) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(fromEmail);

        try {
            System.out.println(" Sending email to: " + to);

            mailSender.send(message);

            System.out.println(" Email sent successfully");

        } catch (Exception e) {
            System.out.println(" EMAIL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
