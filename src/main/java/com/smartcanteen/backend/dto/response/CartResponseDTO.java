package com.smartcanteen.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@Data
public class CartResponseDTO{
    List<CartItemResponseDTO> items;
    BigDecimal totalAmount;
}
