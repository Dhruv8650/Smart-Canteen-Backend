package com.smartcanteen.backend.mapper;

import com.smartcanteen.backend.dto.response.*;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Order;

import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderResponseDTO toDTO(Order order){
        UserResponseDTO userDto = new UserResponseDTO(
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getUser().getRole()
        );

        List<FoodItemResponseDTO> foodDTOs= order.getFoodItems()
                .stream()
                .map(food ->new FoodItemResponseDTO(
                        food.getId(),
                        food.getName(),
                        food.getPrice()
                ))
                .collect(Collectors.toList());

        return new OrderResponseDTO(
          order.getId(),
                userDto,
                foodDTOs,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}
