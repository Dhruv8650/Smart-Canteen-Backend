package com.smartcanteen.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponseDTO {

    private Long id;

    private UserResponseDTO user;

    private List<FoodItemResponseDTO> items;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createdAt;

    // 🔥 NEW FIELDS (must match mapper)

    private String orderNumber;
    private String statusLabel;
    private String formattedDate;
    private int totalItems;
    private String shortDescription;
    private boolean canReorder;
    private boolean canDownloadInvoice;
}