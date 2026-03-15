package com.smartcanteen.backend.mapper;

import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;

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

        return new OrderResponseDTO(
                order.getId(),
                userDTO,
                foodDTOs,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}