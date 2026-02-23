package com.smartcanteen.backend.mapper;

import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Order;

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

        // Map Food Items
        List<FoodItemResponseDTO> foodDTOs = order.getFoodItems()
                .stream()
                .map(food -> new FoodItemResponseDTO(
                        food.getId(),
                        food.getName(),
                        food.getCategory(),
                        food.getPrice(),
                        food.isAvailable()
                ))
                .toList();

        // Return final OrderResponseDTO
        return new OrderResponseDTO(
                order.getId(),
                userDTO,
                foodDTOs,
                order.getTotalAmount(),   // BigDecimal
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}