package com.smartcanteen.backend.dto.response.analytics;

import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CategorySalesDTO{
    FoodCategory category;
    Long totalOrders;
}
