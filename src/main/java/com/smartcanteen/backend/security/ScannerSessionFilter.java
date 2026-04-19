package com.smartcanteen.backend.security;

import com.smartcanteen.backend.service.ScannerSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScannerSessionFilter extends OncePerRequestFilter {

    private final ScannerSessionService service;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println(" Scanner filter HIT: " + request.getServletPath());

        String path = request.getServletPath();

        // Only apply to verify endpoint

        if (!path.equals("/orders/verify") || !"POST".equalsIgnoreCase(request.getMethod())) {


            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {

            String token = auth.substring(7).trim();

            if (token.contains(".")) {
                filterChain.doFilter(request, response);
                return;
            }


            boolean valid = service.isValid(token);

            // For verify endpoint → enforce validity
            if (valid) {
                String managerEmail = service.getManagerEmail(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                managerEmail,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                filterChain.doFilter(request, response);
                return;
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

        }

        // fallback to normal JWT
        filterChain.doFilter(request, response);
    }
}