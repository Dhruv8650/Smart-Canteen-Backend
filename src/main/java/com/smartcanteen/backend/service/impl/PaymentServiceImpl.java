package com.smartcanteen.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.request.PaymentVerifyRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.PaymentAttempt;
import com.smartcanteen.backend.entity.PaymentMethod;
import com.smartcanteen.backend.entity.PaymentStatus;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.OrderMapper;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.PaymentAttemptRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.OrderService;
import com.smartcanteen.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String razorpayWebhookSecret;

    @Override
    @Transactional
    public Map<String, Object> createPaymentOrder(OrderRequestDTO request, String userEmail) {
        if (request.getPaymentMethod() == null || request.getPaymentMethod() == PaymentMethod.CASH) {
            throw new IllegalArgumentException("Online payment requires UPI or CARD");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        BigDecimal amount = orderService.calculateOrderAmount(request, userEmail);
        long amountInPaise = toPaise(amount);

        RazorpayOrderResponse gatewayOrder = createRazorpayOrder(
                amountInPaise,
                "order_" + UUID.randomUUID()
        );

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setUser(user);
        attempt.setRequestJson(writeRequestJson(request));
        attempt.setAmount(amount);
        attempt.setGatewayOrderId(gatewayOrder.id());
        attempt.setStatus(PaymentStatus.INITIATED);
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        PaymentAttempt savedAttempt = paymentAttemptRepository.save(attempt);

        return Map.of(
                "keyId", razorpayKeyId,
                "amount", gatewayOrder.amount(),
                "currency", gatewayOrder.currency(),
                "razorpayOrderId", gatewayOrder.id(),
                "paymentAttemptId", savedAttempt.getId()
        );
    }

    @Override
    @Transactional
    public OrderResponseDTO verifyPayment(PaymentVerifyRequestDTO request, String userEmail) {
        PaymentAttempt attempt = paymentAttemptRepository
                .findByGatewayOrderIdAndUserEmail(request.getRazorpayOrderId(), userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt not found"));

        if (attempt.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(attempt.getExpiresAt()) &&
                attempt.getOrderId() == null) {
            attempt.setStatus(PaymentStatus.FAILED);
            paymentAttemptRepository.save(attempt);
            throw new ResponseStatusException(HttpStatus.GONE, "Payment attempt expired");
        }


        if (!verifySignature(
                attempt.getGatewayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        )) {
            attempt.setStatus(PaymentStatus.FAILED);
            paymentAttemptRepository.save(attempt);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment signature");
        }


        if (attempt.getGatewayPaymentId() != null &&
                !attempt.getGatewayPaymentId().equals(request.getRazorpayPaymentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment attempt already linked to another payment");
        }

        return finalizeSuccessfulPaymentAttempt(
                attempt,
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!verifyWebhookSignature(payload, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        JsonNode root = readJson(payload);
        String event = root.path("event").asText("");

        if (!"payment.captured".equals(event) && !"order.paid".equals(event)) {
            return;
        }

        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        JsonNode orderEntity = root.path("payload").path("order").path("entity");

        String gatewayOrderId = textOrNull(paymentEntity.path("order_id"));
        if (gatewayOrderId == null) {
            gatewayOrderId = textOrNull(orderEntity.path("id"));
        }

        if (gatewayOrderId == null) {
            return;
        }

        PaymentAttempt attempt = paymentAttemptRepository.findByGatewayOrderId(gatewayOrderId)
                .orElse(null);

        if (attempt == null) {
            return;
        }

        finalizeSuccessfulPaymentAttempt(
                attempt,
                textOrNull(paymentEntity.path("id")),
                null
        );
    }

    private RazorpayOrderResponse createRazorpayOrder(long amountInPaise, String receipt) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "amount", amountInPaise,
                    "currency", "INR",
                    "receipt", receipt
            ));

            HttpRequest request = HttpRequest.newBuilder(URI.create(RAZORPAY_ORDERS_URL))
                    .header("Authorization", basicAuthHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create payment order");
            }

            JsonNode body = readJson(response.body());

            return new RazorpayOrderResponse(
                    body.path("id").asText(),
                    body.path("amount").asLong(),
                    body.path("currency").asText("INR")
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create payment order");
        }
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        String expected = hmacHex(orderId + "|" + paymentId, razorpayKeySecret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        String expected = hmacHex(payload, razorpayWebhookSecret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private OrderResponseDTO finalizeSuccessfulPaymentAttempt(PaymentAttempt attempt,
                                                              String paymentId,
                                                              String paymentSignature) {
        if (attempt.getOrderId() != null) {
            return fetchOrderResponse(attempt.getOrderId());
        }

        if (paymentId != null) {
            var existingByOrderId = orderRepository.findByPaymentOrderId(attempt.getGatewayOrderId());
            if (existingByOrderId.isPresent()) {
                attempt.setOrderId(existingByOrderId.get().getId());
                if (paymentId != null) {
                    attempt.setGatewayPaymentId(paymentId);
                }
                if (paymentSignature != null) {
                    attempt.setGatewaySignature(paymentSignature);
                }
                attempt.setStatus(PaymentStatus.SUCCESS);
                paymentAttemptRepository.save(attempt);
                return fetchOrderResponse(existingByOrderId.get().getId());
            }

        }

        attempt.setGatewayPaymentId(paymentId);
        if (paymentSignature != null) {
            attempt.setGatewaySignature(paymentSignature);
        }
        attempt.setStatus(PaymentStatus.SUCCESS);
        paymentAttemptRepository.save(attempt);

        OrderRequestDTO orderRequest = readOrderRequest(attempt.getRequestJson());
        OrderResponseDTO order = orderService.placeVerifiedOnlineOrder(
                orderRequest,
                attempt.getUser().getEmail(),
                attempt.getGatewayOrderId(),
                paymentId,
                paymentSignature,
                attempt.getAmount()
        );

        attempt.setOrderId(order.getId());
        paymentAttemptRepository.save(attempt);

        return order;
    }

    private OrderResponseDTO fetchOrderResponse(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .map(OrderMapper::toDTO)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    private OrderRequestDTO readOrderRequest(String requestJson) {
        try {
            return objectMapper.readValue(requestJson, OrderRequestDTO.class);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid saved payment request");
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment payload");
        }
    }

    private String writeRequestJson(OrderRequestDTO request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save payment request");
        }
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String basicAuthHeader() {
        String credentials = razorpayKeyId + ":" + razorpayKeySecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify payment");
        }
    }

    private String textOrNull(JsonNode node) {
        String value = node.asText("");
        return value.isBlank() ? null : value;
    }

    private record RazorpayOrderResponse(String id, long amount, String currency) {}
}
