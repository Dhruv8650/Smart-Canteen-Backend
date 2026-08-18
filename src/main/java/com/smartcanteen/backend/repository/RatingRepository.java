package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Rating;
import com.smartcanteen.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    interface RatingSummary {
        Long getFoodItemId();
        Double getAverageRating();
        Long getRatingCount();
    }

    Optional<Rating> findByUserAndFoodItem(User user, FoodItem foodItem);

    @Query("""
        SELECT AVG(r.rating)
        FROM Rating r
        WHERE r.foodItem.id = :foodItemId
    """)
    Double getAverageRating(Long foodItemId);

    long countByFoodItemId(Long foodItemId);

    @Query("""
        SELECT r.foodItem.id AS foodItemId,
               AVG(r.rating) AS averageRating,
               COUNT(r.rating) AS ratingCount
        FROM Rating r
        WHERE r.foodItem.id IN :foodItemIds
        GROUP BY r.foodItem.id
    """)
    List<RatingSummary> getRatingSummariesByFoodItemIds(
            Collection<Long> foodItemIds
    );
}