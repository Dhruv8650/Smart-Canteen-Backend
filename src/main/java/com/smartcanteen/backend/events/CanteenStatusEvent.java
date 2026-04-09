package com.smartcanteen.backend.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CanteenStatusEvent {
    private boolean isOpen;
    private String message;
}