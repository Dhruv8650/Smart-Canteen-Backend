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
    private final OrderService orderService;

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
                .orElseGet(() -> createNewCart(user));

        List<CartItem> cartItems = cart.getCartItems() != null
                ? cart.getCartItems()
                : List.of(); // null-safe

        List<CartItemResponseDTO> items = cartItems.stream()
                .filter(ci -> ci.getFoodItem() != null) // safety
                .map(cartItem -> {

                    BigDecimal price = cartItem.getFoodItem().getPrice();
                    int quantity = cartItem.getQuantity();

                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

                    return new CartItemResponseDTO(
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

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        cartItemRepository.delete(cartItem);
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

    //  CHECKOUT
    @Override
    @Transactional
    public OrderResponseDTO checkout(User user, PaymentMethod paymentMethod) {

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getCartItems() == null ||cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        List<OrderItemRequestDTO> items = cart.getCartItems()
                .stream()
                .map(ci -> {
                    OrderItemRequestDTO dto = new OrderItemRequestDTO();
                    dto.setFoodItemId(ci.getFoodItem().getId());
                    dto.setQuantity(ci.getQuantity());
                    return dto;
                })
                .toList();

        OrderRequestDTO request = new OrderRequestDTO();
        request.setItems(items);
        request.setPaymentMethod(paymentMethod);

        OrderResponseDTO response =
                orderService.placeOrder(request, user.getEmail());

        // Clear cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

        return response;
    }

    //  HELPER
    private Cart createNewCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }
}