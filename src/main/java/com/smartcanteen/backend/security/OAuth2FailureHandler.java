package com.smartcanteen.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception
    ) throws IOException, ServletException {

        // Redirect to frontend login page with error
        response.sendRedirect("http://localhost:3000/login?error=google_login_failed");
    }
}