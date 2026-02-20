package com.smartcanteen.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.List;

public class OrderRequestDTO {

    @NotNull(message =" Food item list cannot be null" )
    @NotEmpty(message = "At least one food item must be selected")
    private List<Long> foodItemIds;

    public OrderRequestDTO(List<Long> foodItemIds){
        this.foodItemIds=foodItemIds;
    }

    public List<Long> getFoodItemIds(){
        return foodItemIds;
    }

    public void setFoodItemIds(List<Long> foodItemIds){
        this.foodItemIds=foodItemIds;
    }
}
