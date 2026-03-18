package com.smartcanteen.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Data
public class OrderResponseDTO{
    Long id;
    UserResponseDTO user;
    List<FoodItemResponseDTO> foodItems;
    BigDecimal totalAmount;
    String status;
    LocalDateTime createdAt;
}