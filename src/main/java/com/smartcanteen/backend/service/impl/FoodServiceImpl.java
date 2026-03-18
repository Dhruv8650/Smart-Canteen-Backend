package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.FoodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FoodServiceImpl implements FoodService {

    private final FoodItemRepository foodItemRepository;

    public FoodServiceImpl(FoodItemRepository repository) {
        this.foodItemRepository = repository;
    }

    @Override
    public FoodItemResponseDTO createFood(FoodItemRequestDTO request) {

        log.info("Creating new food item: {}", request.getName());

        FoodItem food = new FoodItem();
        food.setName(request.getName());
        food.setCategory(request.getFoodCategory());
        food.setPrice(request.getPrice());

        FoodItem saved = foodItemRepository.save(food);

        log.info("Food item created with ID: {}", saved.getId());

        return mapToDTO(saved);
    }

    @Override
    public FoodItemResponseDTO updateFood(Long id,
                                          FoodItemRequestDTO request) {

        log.info("Updating food item with ID: {}", id);

        FoodItem food = foodItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Food not found with ID: {}", id);
                    return new FoodNotFoundException("Food not found");
                });

        food.setName(request.getName());
        food.setCategory(request.getFoodCategory());
        food.setPrice(request.getPrice());

        FoodItem updated = foodItemRepository.save(food);

        log.info("Food item updated successfully: {}", id);

        return mapToDTO(updated);
    }

    @Override
    public void deleteFood(Long id) {

        log.info("Deleting food item with ID: {}", id);

        if (!foodItemRepository.existsById(id)) {
            log.error("Food not found for deletion: {}", id);
            throw new FoodNotFoundException("Food not found");
        }

        foodItemRepository.deleteById(id);

        log.info("Food item deleted successfully: {}", id);
    }

    @Override
    public Page<FoodItemResponseDTO> getMenu(
            int page,
            int size,
            String sortBy,
            String direction,
            String foodCategory,
            Boolean available,
            String search) {
        FoodCategory category = null;

        if (foodCategory != null && !foodCategory.isBlank()) {
            try {
                category = FoodCategory.valueOf(foodCategory.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid food category: " + foodCategory);
            }
        }

        log.info("Fetching menu | page={}, size={}, sortBy={}, direction={}, category={}, available={}, search={}",
                page, size, sortBy, direction, foodCategory, available, search);

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FoodItem> foodPage;

        if (search != null && !search.isBlank()) {
            log.info("Applying search filter: {}", search);
            foodPage = foodItemRepository.findByNameContainingIgnoreCase(search, pageable);

        } else if (category != null && available != null) {
            log.info("Filtering by category={} and available={}", category, available);
            foodPage = foodItemRepository.findByCategoryAndAvailable(category, available, pageable);

        } else if (category != null) {
            log.info("Filtering by category={}", category);
            foodPage = foodItemRepository.findByCategory(category, pageable);

        } else if (available != null) {
            log.info("Filtering by availability={}", available);
            foodPage = foodItemRepository.findByAvailable(available, pageable);

        } else {
            log.info("Fetching all menu items");
            foodPage = foodItemRepository.findAll(pageable);
        }

        log.info("Menu fetched successfully with {} items", foodPage.getTotalElements());

        return foodPage.map(this::mapToDTO);
    }

    @Override
    public FoodItemResponseDTO toggleAvailability(Long id){

        log.info("Toggling availability for food ID: {}", id);

        FoodItem food = foodItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Food not found for toggle: {}", id);
                    return new FoodNotFoundException("Food not found");
                });

        food.setAvailable(!food.isAvailable());

        FoodItem updated = foodItemRepository.save(food);

        log.info("Food availability toggled. New status: {}", updated.isAvailable());

        return mapToDTO(updated);
    }

    private FoodItemResponseDTO mapToDTO(FoodItem food) {

        return new FoodItemResponseDTO(
                food.getId(),
                food.getName(),
                food.getCategory(),
                food.getPrice(),
                food.isAvailable()
        );
    }
}