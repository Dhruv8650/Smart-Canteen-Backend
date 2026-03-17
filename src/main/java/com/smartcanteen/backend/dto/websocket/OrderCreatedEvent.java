package com.smartcanteen.backend.dto.websocket;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderCreatedEvent {

    private OrderResponseDTO order;

}