package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        // ✅ Build request body as OBJECT (not string)
        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("sender", Map.of(
                "name", "Smart Canteen",
                "email", "mycanteen00@gmail.com" // must be verified in Brevo
        ));

        requestBody.put("to", List.of(
                Map.of("email", to)
        ));

        requestBody.put("subject", subject);
        requestBody.put("htmlContent", body);
        requestBody.put("textContent", body);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {
            System.out.println(" Sending email via Brevo API...");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    request,
                    String.class
            );

            System.out.println(" Email sent: " + response.getStatusCode());

        } catch (Exception e) {
            System.out.println(" EMAIL ERROR: " + e.getMessage());
            throw new RuntimeException("Email sending failed", e); // 🔥 important
        }
    }
}