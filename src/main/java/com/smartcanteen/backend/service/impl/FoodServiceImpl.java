package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.Category;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.FoodService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class FoodServiceImpl implements FoodService {

    private final FoodItemRepository foodItemRepository;

    public FoodServiceImpl(FoodItemRepository repository) {
        this.foodItemRepository = repository;
    }

    @Override
    public FoodItemResponseDTO createFood(FoodItemRequestDTO request) {

        FoodItem food = new FoodItem();
        food.setName(request.name());
        food.setCategory(request.category());
        food.setPrice(request.price());

        return mapToDTO(foodItemRepository.save(food));
    }

    @Override
    public FoodItemResponseDTO updateFood(Long id,
                                          FoodItemRequestDTO request) {

        FoodItem food = foodItemRepository.findById(id)
                .orElseThrow(() ->
                        new FoodNotFoundException("Food not found"));

        food.setName(request.name());
        food.setCategory(request.category());
        food.setPrice(request.price());

        return mapToDTO(foodItemRepository.save(food));
    }

    @Override
    public void deleteFood(Long id) {

        if (!foodItemRepository.existsById(id)) {
            throw new FoodNotFoundException("Food not found");
        }

        foodItemRepository.deleteById(id);
    }

    @Override
    public Page<FoodItemResponseDTO> getMenu(
            int page,
            int size,
            String sortBy,
            String direction,
            Category category,
            Boolean available,
            String search) {

        Sort sort=direction.equalsIgnoreCase("desc")
                ?Sort.by(sortBy).descending()
                :Sort.by(sortBy).ascending();

        Pageable pageable=PageRequest.of(page,size,sort);

        Page<FoodItem> foodPage;

        // Priority: search -> category+available > category >available > all
        if (search != null && !search.isBlank()){
            foodPage  = foodItemRepository.findByNameContainingIgnoreCase(search,pageable);
        } else if (category !=null && available != null) {
            foodPage = foodItemRepository.findByCategoryAndAvailable(category,available,pageable);
        } else if (category != null) {
            foodPage = foodItemRepository.findByCategory(category,pageable);
        } else if (available != null){
            foodPage = foodItemRepository.findByAvailable(available,pageable);
        } else {
            foodPage = foodItemRepository.findAll(pageable);
        }

        return foodPage.map(this::mapToDTO);
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

    @Override
    public FoodItemResponseDTO toggleAvailability(Long id){
        FoodItem food= foodItemRepository.findById(id)
                .orElseThrow(() -> new FoodNotFoundException("Food not found"));

        food.setAvailable(!food.isAvailable());
        return mapToDTO(foodItemRepository.save(food));
    }
}