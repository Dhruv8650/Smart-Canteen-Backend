package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.FoodItem;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FoodService {
    FoodItemResponseDTO createFood(FoodItemRequestDTO food);

    FoodItemResponseDTO updateFood(Long id,FoodItemRequestDTO food);

    void deleteFood(Long id);

    Page<FoodItemResponseDTO> getMenu(int page,int size);
}
