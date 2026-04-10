package com.smartcanteen.backend.config;

import com.smartcanteen.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // ALWAYS try to get auth from session
        if (accessor.getSessionAttributes() != null) {
            UsernamePasswordAuthenticationToken auth =
                    (UsernamePasswordAuthenticationToken)
                            accessor.getSessionAttributes().get("user");

            if (auth != null) {
                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        //  CONNECT (store auth)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);
                String email = jwtService.extractEmail(token);

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);

                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("user", auth);
                }

                System.out.println("👤 CONNECT USER: " + email);
            }
        }

        return message;
    }
}