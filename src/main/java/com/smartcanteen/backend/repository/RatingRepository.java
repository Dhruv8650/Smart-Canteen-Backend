package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Rating;
import com.smartcanteen.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserAndFoodItem(User user, FoodItem foodItem);

    @Query("""
        SELECT AVG(r.rating)
        FROM Rating r
        WHERE r.foodItem.id = :foodItemId
    """)
    Double getAverageRating(Long foodItemId);


}
