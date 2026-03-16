package com.smartcanteen.backend.dto.request;

import com.smartcanteen.backend.entity.FoodCategory;

import java.math.BigDecimal;

public record FoodItemRequestDTO(
        String name,
        FoodCategory foodCategory,
        BigDecimal price
){}
