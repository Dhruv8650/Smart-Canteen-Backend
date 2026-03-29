package com.smartcanteen.backend.mapper;

import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderMapper {

    public static OrderResponseDTO toDTO(Order order) {

        // Map User
        UserResponseDTO userDTO = new UserResponseDTO(
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getUser().getRole()
        );

        // Map Order Items → Food DTOs
        List<FoodItemResponseDTO> foodDTOs = order.getOrderItems()
                .stream()
                .map(orderItem -> new FoodItemResponseDTO(
                        orderItem.getFoodItem().getId(),
                        orderItem.getFoodItem().getName(),
                        orderItem.getFoodItem().getCategory(),
                        orderItem.getFoodItem().getPrice(),
                        orderItem.getFoodItem().isAvailable()
                ))
                .toList();

        // 🔥 TIME CALCULATION
        Duration duration = Duration.between(order.getCreatedAt(), LocalDateTime.now());

        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds();

        // 🔥 STATUS LABEL (UI LOGIC)
        String statusLabel;

        if (minutes > 10) {
            statusLabel = "DELAYED";
        } else if (minutes > 5) {
            statusLabel = "WARNING";
        } else {
            statusLabel = "ON_TIME";
        }

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
                order.getOrderItems().size(),
                order.getOrderItems().size() + " items • ₹" + order.getTotalAmount(),
                order.getStatus() == OrderStatus.COMPLETED,
                order.getStatus() == OrderStatus.COMPLETED,

                // 🔥 NEW FIELDS
                seconds,
                statusLabel
        );
    }

    // 🔥 FORMAT STATUS
    private static String formatStatus(String status) {
        return switch (status) {
            case "PENDING" -> "Pending";
            case "PREPARING" -> "Preparing";
            case "READY" -> "Ready";
            case "COMPLETED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    // 🔥 FORMAT DATE
    private static String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }
}