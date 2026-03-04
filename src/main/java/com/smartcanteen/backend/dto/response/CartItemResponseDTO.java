package com.smartcanteen.backend.dto.response;

import java.math.BigDecimal;

public record CartItemResponseDTO (
        Long foodItemId,
        String foodName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
){}
