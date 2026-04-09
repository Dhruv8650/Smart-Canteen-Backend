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

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("authorization"); // ✅ fixed

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
                            List.of()
                    );

            accessor.setUser(authentication); // ✅ correct way

            System.out.println("👤 CONNECT USER: " + email);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            if (accessor.getUser() == null) {
                throw new RuntimeException("Unauthorized: No user in session");
            }

            String destination = accessor.getDestination();
            String email = accessor.getUser().getName();

            System.out.println("📡 SUBSCRIBE REQUEST: " + destination + " by " + email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (destination.equals("/topic/admin/orders")) {
                if (user.getRole() != Role.ADMIN &&
                        user.getRole() != Role.MANAGER) {
                    throw new RuntimeException("Access denied");
                }
            }

            if (destination.startsWith("/topic/user/")) {
                Long requestedUserId = Long.parseLong(destination.split("/")[3]);

                if (!user.getId().equals(requestedUserId)) {
                    throw new RuntimeException("Access denied");
                }
            }
        }

        return message;
    }
}