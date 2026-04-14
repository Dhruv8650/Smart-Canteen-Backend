package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Rating;
import com.smartcanteen.backend.entity.User;

public interface RatingService {

    Rating rateItem(User user, Long foodItemId, int rating, String review);

}
