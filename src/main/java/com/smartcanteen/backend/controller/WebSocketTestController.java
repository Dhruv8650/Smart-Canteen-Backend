package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class WebSocketTestController {

    private static final String KITCHEN_TOPIC = "/topic/kitchen/orders";

    private final SimpMessagingTemplate messagingTemplate;

    @RequestMapping(value = "/ws", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendKitchenTestMessage() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "TEST_MESSAGE");
        payload.put("message", "Kitchen WebSocket test message");
        payload.put("destination", KITCHEN_TOPIC);
        payload.put("timestamp", Instant.now().toString());

        try {
            messagingTemplate.convertAndSend(KITCHEN_TOPIC, payload);

            log.info("Test WebSocket message sent to {}", KITCHEN_TOPIC);

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("Test WebSocket message sent")
                            .data(payload)
                            .build()
            );

        } catch (Exception ex) {
            log.error("Failed to send test WebSocket message to {}", KITCHEN_TOPIC, ex);

            payload.put("error", ex.getClass().getSimpleName());

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("WebSocket test message failed; check server logs")
                            .data(payload)
                            .build()
            );
        }
    }
}
