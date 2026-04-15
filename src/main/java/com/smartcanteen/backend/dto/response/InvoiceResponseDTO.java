package com.smartcanteen.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceResponseDTO {
    private Long orderId;
    private String customerName;
    private String email;
    private String status;
    private String date;
    private List<OrderItemDTO> items;
    private Double totalAmount;
    private String pickupCode;
}
