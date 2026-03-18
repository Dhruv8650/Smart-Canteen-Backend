package com.smartcanteen.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AddToCartRequestDTO {
    Long foodItemId;
    Integer quantity;
}
