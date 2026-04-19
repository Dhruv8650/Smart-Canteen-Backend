package com.smartcanteen.backend.security;

import com.smartcanteen.backend.service.JwtService;
import com.smartcanteen.backend.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        System.out.println("REQUEST PATH: " + path);

        if (path.equals("/manager/scanner-session/validate")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.equals("/orders/verify")) {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7).trim();

            if (!token.contains(".")) {
                filterChain.doFilter(request, response);
                return;
            }
        }


        //  Skip public/auth endpoints
        if (
                path.startsWith("/users/register") ||
                        path.startsWith("/users/login") ||
                        path.startsWith("/users/verify-email") ||
                        path.startsWith("/users/verify-otp") ||
                        path.startsWith("/users/resend-otp") ||
                        path.startsWith("/users/forgot-password") ||
                        path.startsWith("/users/reset-password") ||
                        path.startsWith("/users/refresh") ||
                        path.startsWith("/ws-orders")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        //  If no token → continue (Spring will handle if endpoint is protected)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            //  Check blacklist
            if (tokenBlacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token is blacklisted (logged out)");
                return;
            }

            //  Extract email
            String email = jwtService.extractEmail(token);

            if (email != null) {

                //  Load user
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                //  Validate token
                if (jwtService.isTokenValid(token, userDetails)) {

                    //  Create authentication
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    //  ALWAYS SET AUTH (MAIN FIX)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}