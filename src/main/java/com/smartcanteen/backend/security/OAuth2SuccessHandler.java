package com.smartcanteen.backend.security;

import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.JwtService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new RuntimeException("Email not found from Google");
        }

        System.out.println("GOOGLE LOGIN SUCCESS: " + email);

        //  Save user if not exists
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(normalizedEmail);
                    newUser.setName(name);
                    newUser.setRole(Role.USER);
                    newUser.setPassword(UUID.randomUUID().toString());
                    return userRepository.save(newUser);
                });

        //  Generate JWT
        String token = jwtService.generateToken(user.getEmail());

        //  Redirect to frontend
        String redirectUrl = frontendUrl + "/oauth-success?token=" + token;

        response.sendRedirect(redirectUrl);
    }
}