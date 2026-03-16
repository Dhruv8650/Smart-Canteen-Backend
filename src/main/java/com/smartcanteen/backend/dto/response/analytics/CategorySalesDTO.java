package com.smartcanteen.backend.dto.response.analytics;

import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;

public record CategorySalesDTO(
        FoodCategory category,
        Long totalOrders
) {}
