package com.smartcanteen.backend.dto.response.analytics;

import com.smartcanteen.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OrderStatusCountDTO{
    OrderStatus status;
    Long count;
}
