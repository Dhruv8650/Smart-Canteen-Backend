package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.dto.response.CartResponseDTO;
import com.smartcanteen.backend.entity.User;

public interface CartService {
    void addToCart(AddToCartRequestDTO request, User user);

    public CartResponseDTO getCart(User user);

    void removeItem(Long cartItemId,User user);

    void updateQuantity(Long cartItemId,Integer quantity,User user);
}
