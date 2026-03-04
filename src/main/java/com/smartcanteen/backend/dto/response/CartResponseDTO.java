package com.smartcanteen.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDTO(
        List<CartItemResponseDTO> items,
        BigDecimal totalAmount
) {}
