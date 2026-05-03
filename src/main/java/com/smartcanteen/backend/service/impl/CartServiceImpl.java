package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.*;
import com.smartcanteen.backend.dto.response.*;
import com.smartcanteen.backend.entity.*;
import com.smartcanteen.backend.exception.*;
import com.smartcanteen.backend.repository.*;
import com.smartcanteen.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;

    // ADD TO CART
    @Transactional
    @Override
    public void addToCart(AddToCartRequestDTO request, User user) {

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> new FoodNotFoundException("Food item not found"));

        if (!foodItem.isAvailable()) {
            throw new IllegalArgumentException("Food item is not available");
        }

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseGet(() -> createNewCart(user));

        List<CartItem> cartItems = cart.getCartItems();

        if (cartItems == null) {
            cartItems = new ArrayList<>();
            cart.setCartItems(cartItems);
        }

        CartItem cartItem = cartItems.stream()
                .filter(item -> item.getFoodItem().getId().equals(foodItem.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity = request.getQuantity();

        if (cartItem != null) {
            newQuantity = cartItem.getQuantity() + request.getQuantity();
        }

        //  MAX PER ORDER VALIDATION
        if (Boolean.TRUE.equals(foodItem.getIsPreparedItem())) {

            if (foodItem.getMaxPerOrder() != null &&
                    newQuantity > foodItem.getMaxPerOrder()) {

                throw new IllegalArgumentException(
                        "You can only add " + foodItem.getMaxPerOrder() + " " + foodItem.getName() + " to cart"
                );
            }
        }

        if (cartItem != null) {
            cartItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setFoodItem(foodItem);
            newItem.setQuantity(request.getQuantity());

            cart.addItem(newItem);
        }

        cartRepository.save(cart);
    }

    //  GET CART
    @Override
    @Transactional(readOnly = true)
    public CartResponseDTO getCart(User user) {

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElse(null);

        if (cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            return new CartResponseDTO(List.of(), BigDecimal.ZERO);
        }

        List<CartItemResponseDTO> items = cart.getCartItems().stream()
                .filter(ci -> ci.getFoodItem() != null)
                .map(cartItem -> {
                    BigDecimal price = cartItem.getFoodItem().getPrice();
                    int quantity = cartItem.getQuantity();
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

                    return new CartItemResponseDTO(
                            cartItem.getId(),
                            cartItem.getFoodItem().getId(),
                            cartItem.getFoodItem().getName(),
                            price,
                            quantity,
                            subtotal
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponseDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDTO(items, total);
    }


    //  REMOVE ITEM
    @Override
    @Transactional
    public void removeItem(Long cartItemId, User user) {

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if (cartItem.getCart() == null || !cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        cart.removeItem(cartItem);
        cartRepository.saveAndFlush(cart);

        log.info("Cart item removed. cartItemId={}, user={}", cartItemId, user.getEmail());
    }


    //  UPDATE QUANTITY
    @Override
    @Transactional
    public void updateQuantity(Long cartItemId, Integer quantity, User user) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        FoodItem foodItem = cartItem.getFoodItem();

        //  MAX PER ORDER VALIDATION
        if (Boolean.TRUE.equals(foodItem.getIsPreparedItem())) {

            if (foodItem.getMaxPerOrder() != null &&
                    quantity > foodItem.getMaxPerOrder()) {

                throw new IllegalArgumentException(
                        "You can only add " + foodItem.getMaxPerOrder() + " " + foodItem.getName() + " to cart"
                );
            }
        }

        cartItem.setQuantity(quantity);
    }


    @Transactional
    @Override
    public void clearCart(User user) {

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElse(null);

        if (cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            log.info("Cart already empty for user: {}", user.getEmail());
            return;
        }

        int removedCount = cart.getCartItems().size();

        new ArrayList<>(cart.getCartItems())
                .forEach(cart::removeItem);

        cartRepository.saveAndFlush(cart);

        log.info("Cart cleared for user: {}, removedItems={}", user.getEmail(), removedCount);
    }


    //  HELPER
    private Cart createNewCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }
}