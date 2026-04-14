package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Rating;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.RatingRepository;
import com.smartcanteen.backend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final FoodItemRepository foodItemRepository;
    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;

    @Override
    public Rating rateItem(User user, Long foodItemId, int ratingValue, String review) {

        //  CHECK: user has ordered this item
        boolean hasOrdered = orderRepository.hasUserOrderedItem(user, foodItemId);

        if (!hasOrdered) {
            throw new RuntimeException("You can only rate items you have ordered");
        }

        FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new RuntimeException("Food item not found"));

        Optional<Rating> existing = ratingRepository.findByUserAndFoodItem(user, foodItem);

        if (existing.isPresent()) {
            Rating r = existing.get();
            r.setRating(ratingValue);
            r.setReview(review);
            return ratingRepository.save(r);
        }

        Rating rating = Rating.builder()
                .user(user)
                .foodItem(foodItem)
                .rating(ratingValue)
                .review(review)
                .build();

        return ratingRepository.save(rating);
    }
}
