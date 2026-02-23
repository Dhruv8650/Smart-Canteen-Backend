package com.smartcanteen.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        UserResponseDTO user,
        List<FoodItemResponseDTO> foodItems,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt
) {}