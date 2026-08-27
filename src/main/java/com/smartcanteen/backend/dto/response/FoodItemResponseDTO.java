package com.smartcanteen.backend.dto.response;

import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.ItemType;
import com.smartcanteen.backend.entity.KitchenResourceType;
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
    Integer maxPerOrder;
    Double averageRating;
    long ratingCount;
    ItemType itemType;
    int prepTimeMinutes;
    Boolean isPreparedItem;
    private KitchenResourceType requiredResource;
}