package com.smartcanteen.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {

    private final Long id;
    private final UserResponseDTO user;
    private final List<FoodItemResponseDTO> fooditems;
    private final double totalAmount;
    private final String status;
    private final LocalDateTime createdAt;

    public OrderResponseDTO(Long id, UserResponseDTO user, List<FoodItemResponseDTO> fooditems, double totalAmount, String status, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.fooditems = fooditems;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public List<FoodItemResponseDTO> getFoodItems() {
        return fooditems;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
