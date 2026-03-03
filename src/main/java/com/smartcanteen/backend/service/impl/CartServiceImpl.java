package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.entity.Cart;
import com.smartcanteen.backend.entity.CartItem;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.repository.CartItemRepository;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;

    @Transactional
    @Override
    public void addToCart(AddToCartRequestDTO request, User user) {
        if(request.quantity() == null || request.quantity() <= 0){
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        FoodItem foodItem=foodItemRepository.findById(request.foodItemId())
                .orElseThrow(() -> new FoodNotFoundException("Food item not found"));

        if(!foodItem.isAvailable()){
            throw new IllegalArgumentException("Food Item is not available");
        }

        Cart cart= cartRepository.findByUser(user)
                .orElseGet(()->{
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem=cart.getCartItems()
                .stream()
                .filter(item ->item.getFoodItem().getId().equals(foodItem.getId()))
                .findFirst()
                .orElse(null);

        if(cartItem !=null){
            cartItem.setQuantity(cartItem.getQuantity()+ request.quantity());
        }else {
            CartItem newItem=new CartItem();
            newItem.setFoodItem(foodItem);
            newItem.setQuantity(request.quantity());

            cart.addItem(newItem);
            cartRepository.save(cart);

        }

    }



}
