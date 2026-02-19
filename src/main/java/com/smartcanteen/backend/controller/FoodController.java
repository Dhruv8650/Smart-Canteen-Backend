package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.service.FoodService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.util.List;

@RestController
@RequestMapping("/food")
public class FoodController {
    private final FoodService foodService;

    public FoodController(FoodService foodService){
        this.foodService=foodService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FoodItem createFood(@RequestBody FoodItem food){
        return foodService.createFood(food);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FoodItem updateFood(@PathVariable Long id,@RequestBody FoodItem food){
        return foodService.updateFood(id,food);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteFood(@PathVariable Long id){
        foodService.deleteFood(id);
        return "Food deleted successfully";
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<FoodItem> getAllFood(){
        return foodService.getAllFood();
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<FoodItem> getAvailableFood(){
        return foodService.getAvailableFood();
    }
}
