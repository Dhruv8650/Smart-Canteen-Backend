package com.smartcanteen.backend.dto.request;

import com.smartcanteen.backend.entity.FoodCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class FoodItemRequestDTO{
    String name;
    FoodCategory foodCategory;
    BigDecimal price;
    String imageUrl;
    private Boolean isPreparedItem;
    private Integer maxPerOrder;
}
