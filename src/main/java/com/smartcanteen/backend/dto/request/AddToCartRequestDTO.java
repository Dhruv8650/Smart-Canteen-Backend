package com.smartcanteen.backend.dto.request;

public record AddToCartRequestDTO(
        Long foodItemId,
        Integer quantity
) {}
