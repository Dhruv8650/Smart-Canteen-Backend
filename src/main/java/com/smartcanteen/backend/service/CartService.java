package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.entity.User;

public interface CartService {
    void addToCart(AddToCartRequestDTO request, User user);
}
