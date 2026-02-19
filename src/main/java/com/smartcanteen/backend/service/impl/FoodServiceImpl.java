package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.FoodService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodServiceImpl implements FoodService {
    private final FoodItemRepository foodItemRepository;

    public FoodServiceImpl(FoodItemRepository foodItemRepository){
            this.foodItemRepository=foodItemRepository;
    }

    @Override
    public FoodItem createFood(FoodItem food){
        return foodItemRepository.save(food);
    }

    @Override
    public FoodItem updateFood(Long id, FoodItem food) {
        FoodItem existingFood = foodItemRepository.findById(id)
                .orElseThrow(()-> new FoodNotFoundException("Food not found"));

        existingFood.setName(food.getName());
        existingFood.setCategory(food.getCategory());
        existingFood.setPrice(food.getPrice());
        existingFood.setAvailable(food.isAvailable());

        return foodItemRepository.save(existingFood);
    }

    @Override
    public void deleteFood(Long id) {
        if(!foodItemRepository.existsById(id)){
            throw new FoodNotFoundException("Food not found");
        }
        foodItemRepository.deleteById(id);
    }

    @Override
    public List<FoodItem> getAllFood() {
        return foodItemRepository.findAll();
    }

    @Override
    public List<FoodItem> getAvailableFood() {
        return foodItemRepository.findByAvailableTrue();
    }
}
