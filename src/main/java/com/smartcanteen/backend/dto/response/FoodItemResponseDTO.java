package com.smartcanteen.backend.dto.response;

import com.smartcanteen.backend.entity.FoodCategory;

import java.math.BigDecimal;

public record FoodItemResponseDTO(
        Long id,
        String name,
        FoodCategory foodCategory,
        BigDecimal price,
        boolean available
) {}