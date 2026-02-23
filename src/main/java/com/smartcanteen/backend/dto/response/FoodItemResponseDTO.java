package com.smartcanteen.backend.dto.response;

import com.smartcanteen.backend.entity.Category;

import java.math.BigDecimal;

public record FoodItemResponseDTO(
        Long id,
        String name,
        Category category,
        BigDecimal price,
        boolean available
) {}