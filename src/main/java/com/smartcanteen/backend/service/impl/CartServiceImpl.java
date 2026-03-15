package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.dto.response.CartItemResponseDTO;
import com.smartcanteen.backend.dto.response.CartResponseDTO;
import com.smartcanteen.backend.entity.Cart;
import com.smartcanteen.backend.entity.CartItem;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.CartItemNotFoundException;
import com.smartcanteen.backend.exception.CartNotFoundException;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.repository.CartItemRepository;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    @Override
    public CartResponseDTO getCart(User user){
        Cart cart=cartRepository.findByUser(user)
                .orElseThrow(()-> new CartNotFoundException("Cart not found"));

        List<CartItemResponseDTO> items=cart.getCartItems()
                .stream()
                .map(cartItem -> {
                    BigDecimal subtotal=cartItem.getFoodItem()
                            .getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                    return new CartItemResponseDTO(
                            cartItem.getFoodItem().getId(),
                            cartItem.getFoodItem().getName(),
                            cartItem.getFoodItem().getPrice(),
                            cartItem.getQuantity(),
                            subtotal
                    );
                })
                .toList();
        BigDecimal total=items.stream()
                .map(CartItemResponseDTO::subtotal)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        return new CartResponseDTO(items,total);
    }

    @Override
    @Transactional
    public void removeItem(Long cartItemId,User user){
        Cart cart=cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem cartItem=cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if(!cartItem.getCart().getId().equals(cart.getId())){
            throw new RuntimeException("Unauthorized action");
        }
        cartItemRepository.delete(cartItem);
    }

}
