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

        //  HANDLE CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // FIX: Support both header cases
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }

            //  DEBUG (optional - remove later)
            System.out.println("Headers: " + accessor.toNativeHeaderMap());

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            String email = jwtService.extractEmail(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            //  IMPROVED: Add role as authority
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(() -> "ROLE_" + user.getRole().name())
                    );

            accessor.setUser(authentication);

            System.out.println("👤 CONNECT USER: " + email);
        }

        // HANDLE SUBSCRIBE
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            if (accessor.getUser() == null) {
                throw new RuntimeException("Unauthorized: No user in session");
            }

            String destination = accessor.getDestination();

            if (destination == null) {
                return message;
            }

            User user = (User) ((UsernamePasswordAuthenticationToken) accessor.getUser()).getPrincipal();

            System.out.println("📡 SUBSCRIBE REQUEST: [" + destination + "] by " + user.getEmail());
            System.out.println("ROLE: " + user.getRole());

            //  SAFE MATCH (IMPORTANT)
            if (destination.contains("/topic/orders")) {
                if (!(user.getRole() == Role.ADMIN ||
                        user.getRole() == Role.MANAGER ||
                        user.getRole() == Role.KITCHEN)) {

                    throw new RuntimeException("Access denied");
                }

                return message; //  allow
            }

            //  ADMIN TOPIC
            if (destination.contains("/topic/admin/orders")) {
                if (!(user.getRole() == Role.ADMIN ||
                        user.getRole() == Role.MANAGER)) {

                    throw new RuntimeException("Access denied");
                }

                return message;
            }

            //  USER TOPIC (SAFE PARSE)
            if (destination.contains("/topic/user/")) {
                try {
                    Long requestedUserId = Long.parseLong(destination.split("/")[3]);

                    if (!user.getId().equals(requestedUserId)) {
                        throw new RuntimeException("Access denied");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Invalid destination format");
                }
            }
        }

        return message;
    }
}