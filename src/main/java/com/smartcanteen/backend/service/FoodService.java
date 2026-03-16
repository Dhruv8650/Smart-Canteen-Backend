package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.FoodCategory;
import org.springframework.data.domain.Page;

public interface FoodService {
    FoodItemResponseDTO createFood(FoodItemRequestDTO food);

    FoodItemResponseDTO updateFood(Long id,FoodItemRequestDTO food);

    void deleteFood(Long id);

    Page<FoodItemResponseDTO> getMenu(
            int page,
            int size,
            String sortBy,
            String direction,
            FoodCategory foodCategory,
            Boolean available,
            String search);

    public FoodItemResponseDTO toggleAvailability(Long id);
}
