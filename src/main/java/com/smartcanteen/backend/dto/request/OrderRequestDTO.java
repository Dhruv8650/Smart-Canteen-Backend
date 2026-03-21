package com.smartcanteen.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDTO {

    @NotNull(message ="Food item list cannot be null")
    @NotEmpty(message = "At least one food item must be selected")
    private List<OrderItemRequestDTO> items;

}