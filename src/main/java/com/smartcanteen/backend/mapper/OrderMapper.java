package com.smartcanteen.backend.mapper;

import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderMapper {

    public static OrderResponseDTO toDTO(Order order) {

        // USER MAPPING
        UserResponseDTO userDTO = new UserResponseDTO(
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getUser().getRole(),
                order.getUser().isActive()
        );

        // ORDER ITEMS → FOOD DTOs
        List<FoodItemResponseDTO> foodDTOs = order.getOrderItems()
                .stream()
                .map(orderItem -> {
                    var food = orderItem.getFoodItem();

                    return new FoodItemResponseDTO(
                            food.getId(),
                            food.getName(),
                            food.getCategory(),
                            food.getPrice(),
                            food.isAvailable(),
                            food.getImageUrl(),
                            food.getIsPreparedItem(),
                            food.getMaxPerOrder()
                    );
                })
                .toList();

        // TIME CALCULATION
        Duration duration = Duration.between(order.getCreatedAt(), LocalDateTime.now(ZoneOffset.UTC));

        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        // STATUS LABEL
        String statusLabel;

        if (minutes > 10) {
            statusLabel = "DELAYED";
        } else if (minutes > 5) {
            statusLabel = "WARNING";
        } else {
            statusLabel = "ON_TIME";
        }

        // ITEM SUMMARY
        int itemCount = order.getOrderItems().size();
        String summary = itemCount +
                (itemCount == 1 ? " item • ₹" : " items • ₹") +
                order.getTotalAmount();

        //  QR + INVOICE LOGIC

        boolean showQr = order.getStatus() == OrderStatus.READY;

        String pickupCode = showQr
                ? order.getPickupCode()   // only expose when READY
                : null;

        return new OrderResponseDTO(
                order.getId(),
                userDTO,
                foodDTOs,
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),

                // EXISTING FIELDS
                "ORD-" + order.getId(),
                formatStatus(order.getStatus().name()),
                formatDate(order.getCreatedAt()),
                itemCount,
                summary,
                order.getStatus() == OrderStatus.COMPLETED,
                order.getStatus() == OrderStatus.COMPLETED,

                // TIME FIELDS
                seconds,
                statusLabel,

                // QR
                pickupCode,
                showQr
        );
    }

    // FORMAT STATUS
    private static String formatStatus(String status) {
        return switch (status) {
            case "PENDING" -> "Pending";
            case "PAYMENT_PENDING" -> "Payment Pending";
            case "PREPARING" -> "Preparing";
            case "READY" -> "Ready";
            case "COMPLETED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    // FORMAT DATE
    private static String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }
}