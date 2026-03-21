package com.smartcanteen.backend.dto.request;

import com.smartcanteen.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateOrderStatusDTO {
    private OrderStatus status;
}