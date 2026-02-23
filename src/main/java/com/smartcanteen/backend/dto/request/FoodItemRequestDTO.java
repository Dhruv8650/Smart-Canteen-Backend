package com.smartcanteen.backend.dto.request;

import com.smartcanteen.backend.entity.Category;

import java.math.BigDecimal;

public record FoodItemRequestDTO(
        String name,
        Category category,
        BigDecimal price
){}
