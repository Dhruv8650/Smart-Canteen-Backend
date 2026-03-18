package com.smartcanteen.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class CartItemResponseDTO{
    Long foodItemId;
    String foodName;
    BigDecimal price;
    int quantity;
    BigDecimal subtotal;
}
