package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.dto.request.OrderItemRequestDTO;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.CartItemResponseDTO;
import com.smartcanteen.backend.dto.response.CartResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.*;
import com.smartcanteen.backend.exception.CartItemNotFoundException;
import com.smartcanteen.backend.exception.CartNotFoundException;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.repository.CartItemRepository;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.service.CartService;
import com.smartcanteen.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderService orderService;

    // 🔹 ADD TO CART
    @Transactional
    @Override
    public void addToCart(AddToCartRequestDTO request, User user) {

        log.info("User {} adding item {} to cart", user.getEmail(), request.getFoodItemId());

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> new FoodNotFoundException("Food item not found"));

        if (!foodItem.isAvailable()) {
            throw new IllegalArgumentException("Food Item is not available");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem = cart.getCartItems()
                .stream()
                .filter(item -> item.getFoodItem().getId().equals(foodItem.getId()))
                .findFirst()
                .orElse(null);

        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setFoodItem(foodItem);
            newItem.setQuantity(request.getQuantity());

            cart.addItem(newItem);
            cartRepository.save(cart);
        }
    }

    // 🔹 GET CART
    @Override
    public CartResponseDTO getCart(User user) {

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        List<CartItemResponseDTO> items = cart.getCartItems()
                .stream()
                .map(cartItem -> {
                    BigDecimal subtotal = cartItem.getFoodItem()
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

        BigDecimal total = items.stream()
                .map(CartItemResponseDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDTO(items, total);
    }

    // 🔹 REMOVE ITEM
    @Override
    @Transactional
    public void removeItem(Long cartItemId, User user) {

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        cartItemRepository.delete(cartItem);
    }

    // 🔹 UPDATE QUANTITY
    @Override
    @Transactional
    public void updateQuantity(Long cartItemId, Integer quantity, User user) {

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        cartItem.setQuantity(quantity);
    }

    // 🔥 FINAL CHECKOUT (CLEAN ARCHITECTURE)
    @Override
    @Transactional
    public OrderResponseDTO checkout(User user) {

        log.info("Checkout started for user: {}", user.getEmail());

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 🔹 Convert cart → OrderRequestDTO
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

        // 🔥 Delegate to OrderService
        OrderResponseDTO response = orderService.placeOrder(request, user.getEmail());

        log.info("Order placed successfully with ID: {}", response.getId());

        // 🔹 Clear cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

        log.info("Cart cleared after checkout for user: {}", user.getEmail());

        return response;
    }
}