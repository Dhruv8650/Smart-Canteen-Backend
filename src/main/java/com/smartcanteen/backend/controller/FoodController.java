package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.Category;
import com.smartcanteen.backend.service.FoodService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;

@RestController
@RequestMapping("/menu")
public class FoodController {

    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FoodItemResponseDTO createFood(
            @RequestBody FoodItemRequestDTO request) {
        return service.createFood(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FoodItemResponseDTO updateFood(
            @PathVariable Long id,
            @RequestBody FoodItemRequestDTO request) {
        return service.updateFood(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteFood(@PathVariable Long id) {
        service.deleteFood(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<FoodItemResponseDTO> getMenu(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String search) {
        return service.getMenu(page, size,sortBy,direction,category,available,search);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN','MANAGER')")
    public FoodItemResponseDTO toggleAvailability(@PathVariable Long id){
        return service.toggleAvailability(id);
    }
}