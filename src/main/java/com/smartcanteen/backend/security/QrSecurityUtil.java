package com.smartcanteen.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class QrSecurityUtil {

    private final String secret;

    public QrSecurityUtil(@Value("${qr.secret}") String secret) {
        this.secret = secret;
    }

    public String generateSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key =
                    new SecretKeySpec(secret.getBytes(), "HmacSHA256");

            mac.init(key);

            byte[] rawHmac = mac.doFinal(data.getBytes());

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Error generating signature");
        }
    }

    public boolean verify(String data, String signature) {
        return generateSignature(data).equals(signature);
    }
}