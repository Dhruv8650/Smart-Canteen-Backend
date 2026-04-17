package com.smartcanteen.backend.security;

import com.smartcanteen.backend.service.ScannerSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ScannerSessionFilter extends OncePerRequestFilter {

    private final ScannerSessionService service;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Only apply to verify endpoint
        if (!path.equals("/orders/verify")) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {

            String token = auth.substring(7);

            if (service.isValid(token)) {
                // allow request (scanner auth)
                filterChain.doFilter(request, response);
                return;
            }
        }

        // fallback to normal JWT
        filterChain.doFilter(request, response);
    }
}