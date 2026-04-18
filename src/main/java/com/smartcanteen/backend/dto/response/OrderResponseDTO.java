package com.smartcanteen.backend.dto.response;

import com.smartcanteen.backend.entity.OrderStatus;
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

    private OrderStatus status;

    private LocalDateTime createdAt;

    private String orderType;

    private String orderNumber;
    private String statusLabel; // business status
    private String formattedDate;
    private int totalItems;
    private String shortDescription;
    private boolean canReorder;
    private boolean canDownloadInvoice;
    private long elapsedSeconds;
    private String timeStatus; // WARNING / DELAYED / ON_TIME
    private String pickupCode;
    private boolean showQr;

}