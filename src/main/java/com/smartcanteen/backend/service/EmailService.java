package com.smartcanteen.backend.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
