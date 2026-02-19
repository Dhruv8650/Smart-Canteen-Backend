package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.FoodItem;

import java.util.List;

public interface FoodService {
    FoodItem createFood(FoodItem food);

    FoodItem updateFood(Long id,FoodItem food);

    void deleteFood(Long id);

    List<FoodItem> getAllFood();

    List<FoodItem> getAvailableFood();
}
