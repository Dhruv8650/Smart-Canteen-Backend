package com.smartcanteen.backend.dto.request;

import com.smartcanteen.backend.entity.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDTO {

    private List<OrderItemRequestDTO> items;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

}