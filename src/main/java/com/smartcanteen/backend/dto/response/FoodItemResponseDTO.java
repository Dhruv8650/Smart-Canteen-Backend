package com.smartcanteen.backend.dto.response;

import com.smartcanteen.backend.entity.FoodCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class FoodItemResponseDTO{
    Long id;
    String name;
    FoodCategory foodCategory;
    BigDecimal price;
    boolean available;
    String imageUrl;
    Boolean isPreparedItem;
    Integer maxPerOrder;
}