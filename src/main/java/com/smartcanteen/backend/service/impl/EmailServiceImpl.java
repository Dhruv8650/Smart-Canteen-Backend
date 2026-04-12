package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendEmail(String to, String subject, String body) {

        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
        {
          "sender": { "email": "mycanteen00@gmail.com", "name": "Smart Canteen" },
          "to": [{ "email": "%s" }],
          "subject": "%s",
          "htmlContent": "%s",
          "textContent": "%s"
        }
        """.formatted(to, subject, body, body);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("📧 Sending email via Brevo API...");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    request,
                    String.class
            );

            System.out.println(" Email sent: " + response.getStatusCode());

        } catch (Exception e) {
            System.out.println(" EMAIL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}