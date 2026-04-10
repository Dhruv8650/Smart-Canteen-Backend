package com.smartcanteen.backend.config;

import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        //  RESTORE USER FIRST
        if (accessor.getSessionAttributes() != null) {
            Object sessionUser = accessor.getSessionAttributes().get("user");

            if (sessionUser != null) {
                accessor.setUser((UsernamePasswordAuthenticationToken) sessionUser);
            }
        }

        //  CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(() -> "ROLE_" + user.getRole().name())
                    );

            accessor.setUser(authentication);

            //  STORE IN SESSION
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("user", authentication);
            }

            System.out.println("👤 CONNECT USER: " + email);
        }

        //  SUBSCRIBE
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            UsernamePasswordAuthenticationToken authentication =
                    (UsernamePasswordAuthenticationToken) accessor.getUser();

            if (authentication == null) {
                throw new RuntimeException("Unauthorized: No user in session");
            }

            User user = (User) authentication.getPrincipal();

            String destination = accessor.getDestination();

            System.out.println("📡 SUBSCRIBE REQUEST: [" + destination + "] by " + user.getEmail());

            if (destination != null && destination.contains("/topic/orders")) {
                if (!(user.getRole() == Role.ADMIN ||
                        user.getRole() == Role.MANAGER ||
                        user.getRole() == Role.KITCHEN)) {

                    throw new RuntimeException("Access denied");
                }
            }
        }

        return message;
    }
}