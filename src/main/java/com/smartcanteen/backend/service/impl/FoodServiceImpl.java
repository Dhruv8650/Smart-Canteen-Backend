package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.FoodService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class FoodServiceImpl implements FoodService {

    private final FoodItemRepository repository;

    public FoodServiceImpl(FoodItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public FoodItemResponseDTO createFood(FoodItemRequestDTO request) {

        FoodItem food = new FoodItem();
        food.setName(request.name());
        food.setCategory(request.category());
        food.setPrice(request.price());

        return mapToDTO(repository.save(food));
    }

    @Override
    public FoodItemResponseDTO updateFood(Long id,
                                          FoodItemRequestDTO request) {

        FoodItem food = repository.findById(id)
                .orElseThrow(() ->
                        new FoodNotFoundException("Food not found"));

        food.setName(request.name());
        food.setCategory(request.category());
        food.setPrice(request.price());

        return mapToDTO(repository.save(food));
    }

    @Override
    public void deleteFood(Long id) {

        if (!repository.existsById(id)) {
            throw new FoodNotFoundException("Food not found");
        }

        repository.deleteById(id);
    }

    @Override
    public Page<FoodItemResponseDTO> getMenu(int page, int size) {

        return repository.findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
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