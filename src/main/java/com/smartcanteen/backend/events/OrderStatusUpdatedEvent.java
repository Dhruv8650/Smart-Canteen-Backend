package com.smartcanteen.backend.events;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderStatusUpdatedEvent {

    private final OrderResponseDTO order;
}