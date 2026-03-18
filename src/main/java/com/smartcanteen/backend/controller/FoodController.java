package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.FoodItemRequestDTO;
import com.smartcanteen.backend.dto.response.FoodItemResponseDTO;
import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService service;

    //  CREATE FOOD (ADMIN / MANAGER)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<FoodItemResponseDTO>> createFood(
            @RequestBody FoodItemRequestDTO request) {

        FoodItemResponseDTO item = service.createFood(request);

        ApiResponse<FoodItemResponseDTO> response =
                ApiResponse.<FoodItemResponseDTO>builder()
                        .success(true)
                        .message("Food item created successfully")
                        .data(item)
                        .build();

        return ResponseEntity.ok(response);
    }

    //  UPDATE FOOD
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<FoodItemResponseDTO>> updateFood(
            @PathVariable Long id,
            @RequestBody FoodItemRequestDTO request) {

        FoodItemResponseDTO item = service.updateFood(id, request);

        ApiResponse<FoodItemResponseDTO> response =
                ApiResponse.<FoodItemResponseDTO>builder()
                        .success(true)
                        .message("Food item updated successfully")
                        .data(item)
                        .build();

        return ResponseEntity.ok(response);
    }

    //  DELETE FOOD
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {

        service.deleteFood(id);

        ApiResponse<Void> response =
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Food item deleted successfully")
                        .data(null)
                        .build();

        return ResponseEntity.ok(response);
    }

    //  GET MENU -> PUBLIC
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FoodItemResponseDTO>>> getMenu(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String foodCategory,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String search) {

        Page<FoodItemResponseDTO> items =
                service.getMenu(page, size, sortBy, direction, foodCategory, available, search);

        ApiResponse<Page<FoodItemResponseDTO>> response =
                ApiResponse.<Page<FoodItemResponseDTO>>builder()
                        .success(true)
                        .message("Menu fetched successfully")
                        .data(items)
                        .build();

        return ResponseEntity.ok(response);
    }

    //  TOGGLE AVAILABILITY
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<FoodItemResponseDTO>> toggleAvailability(
            @PathVariable Long id) {

        FoodItemResponseDTO item = service.toggleAvailability(id);

        ApiResponse<FoodItemResponseDTO> response =
                ApiResponse.<FoodItemResponseDTO>builder()
                        .success(true)
                        .message("Food availability updated successfully")
                        .data(item)
                        .build();

        return ResponseEntity.ok(response);
    }
}