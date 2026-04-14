package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.RatingService;
import com.smartcanteen.backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/ratings")
@RestController
@AllArgsConstructor
public class RatingController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RatingService ratingService;

    @PostMapping("/rate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Object>> rateItem(
            @RequestParam Long foodItemId,
            @RequestParam int rating,
            @RequestParam(required = false) String review,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        ratingService.rateItem(user, foodItemId, rating, review);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Rating submitted successfully")
                        .build()
        );
    }
}
