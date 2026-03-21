package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.dto.response.CartItemResponseDTO;
import com.smartcanteen.backend.dto.response.CartResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.*;
import com.smartcanteen.backend.exception.CartItemNotFoundException;
import com.smartcanteen.backend.exception.CartNotFoundException;
import com.smartcanteen.backend.exception.FoodNotFoundException;
import com.smartcanteen.backend.mapper.OrderMapper;
import com.smartcanteen.backend.repository.CartItemRepository;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void addToCart(AddToCartRequestDTO request, User user) {

        log.info("User {} adding item {} to cart", user.getEmail(), request.getFoodItemId());

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            log.warn("Invalid quantity: {}", request.getQuantity());
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> {
                    log.error("Food item not found: {}", request.getFoodItemId());
                    return new FoodNotFoundException("Food item not found");
                });

        if (!foodItem.isAvailable()) {
            log.warn("Food item not available: {}", foodItem.getId());
            throw new IllegalArgumentException("Food Item is not available");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating new cart for user: {}", user.getEmail());
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
            log.info("Updating quantity for foodId {}", foodItem.getId());
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        } else {
            log.info("Adding new item to cart: foodId {}", foodItem.getId());
            CartItem newItem = new CartItem();
            newItem.setFoodItem(foodItem);
            newItem.setQuantity(request.getQuantity());

            cart.addItem(newItem);
            cartRepository.save(cart);
        }

        log.info("Cart updated successfully for user: {}", user.getEmail());
    }

    @Override
    public CartResponseDTO getCart(User user) {

        log.info("Fetching cart for user: {}", user.getEmail());

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for user: {}", user.getEmail());
                    return new CartNotFoundException("Cart not found");
                });

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

        log.info("Cart fetched successfully with {} items", items.size());

        return new CartResponseDTO(items, total);
    }

    @Override
    @Transactional
    public void removeItem(Long cartItemId, User user) {

        log.info("Removing cart item {} for user {}", cartItemId, user.getEmail());

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for user: {}", user.getEmail());
                    return new CartNotFoundException("Cart not found");
                });

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.error("Cart item not found: {}", cartItemId);
                    return new CartItemNotFoundException("Cart item not found");
                });

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            log.error("Unauthorized cart item access: {}", cartItemId);
            throw new RuntimeException("Unauthorized action");
        }

        cartItemRepository.delete(cartItem);

        log.info("Cart item removed successfully: {}", cartItemId);
    }

    @Override
    @Transactional
    public void updateQuantity(Long cartItemId, Integer quantity, User user) {

        log.info("Updating quantity for cartItem {} to {}", cartItemId, quantity);

        if (quantity == null || quantity <= 0) {
            log.warn("Invalid quantity: {}", quantity);
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for user: {}", user.getEmail());
                    return new CartNotFoundException("Cart not found");
                });

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.error("Cart item not found: {}", cartItemId);
                    return new CartItemNotFoundException("Cart item not found");
                });

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            log.error("Unauthorized quantity update attempt: {}", cartItemId);
            throw new RuntimeException("Unauthorized action");
        }

        cartItem.setQuantity(quantity);

        log.info("Cart item quantity updated successfully: {}", cartItemId);
    }

    @Override
    @Transactional
    public void checkout(User user) {

        log.info("Checkout started for user: {}", user.getEmail());

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for user: {}", user.getEmail());
                    return new RuntimeException("Cart not found");
                });

        if (cart.getCartItems().isEmpty()) {
            log.warn("Cart is empty for user: {}", user.getEmail());
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = cart.getCartItems()
                .stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setFoodItem(cartItem.getFoodItem());
                    orderItem.setQuantity(cartItem.getQuantity());
                    return orderItem;
                })
                .toList();

        order.setOrderItems(orderItems);

        BigDecimal total = cart.getCartItems()
                .stream()
                .map(item -> item.getFoodItem()
                        .getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        log.info("Order total calculated: {}", total);

        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully with ID: {}", savedOrder.getId());

        OrderResponseDTO response = OrderMapper.toDTO(savedOrder);

        log.info("Publishing order created event for orderId: {}", savedOrder.getId());
        eventPublisher.publishEvent(new OrderCreatedEvent(response));

        cart.getCartItems().clear();
        cartRepository.save(cart);

        log.info("Cart cleared after checkout for user: {}", user.getEmail());
    }
}