package com.smartcanteen.backend.repository;


import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem,Long> {

    Page<FoodItem> findByCategory(FoodCategory foodCategory, Pageable pageable);

    Page<FoodItem> findByAvailable(Boolean available, Pageable pageable);

    Page<FoodItem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<FoodItem> findByCategoryAndAvailable(
            FoodCategory foodCategory,
            Boolean available,
            Pageable pageable
    );


}
