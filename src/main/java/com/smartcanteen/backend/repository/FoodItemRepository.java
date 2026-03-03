package com.smartcanteen.backend.repository;


import com.smartcanteen.backend.entity.Category;
import com.smartcanteen.backend.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem,Long> {

    Page<FoodItem> findByCategory(Category category, Pageable pageable);

    Page<FoodItem> findByAvailable(Boolean available, Pageable pageable);

    Page<FoodItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<FoodItem> findByCategoryAndAvailable(
            Category category,
            Boolean available,
            Pageable pageable
    );


}
