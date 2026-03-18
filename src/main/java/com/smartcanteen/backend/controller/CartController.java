package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.dto.request.UpdateCartItemRequestDTO;
import com.smartcanteen.backend.dto.response.CartResponseDTO;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    //  COMMON METHOD -> reduces duplication
    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    //  ADD TO CART
    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @RequestBody AddToCartRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        cartService.addToCart(request, user);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Item added to cart successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    //  GET CART
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        CartResponseDTO cart = cartService.getCart(user);

        ApiResponse<CartResponseDTO> response = ApiResponse.<CartResponseDTO>builder()
                .success(true)
                .message("Cart fetched successfully")
                .data(cart)
                .build();

        return ResponseEntity.ok(response);
    }

    //  REMOVE ITEM
    @DeleteMapping("/item/{cartItemId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        cartService.removeItem(cartItemId, user);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Item removed from cart successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    //  UPDATE QUANTITY
    @PutMapping("/item/{cartItemId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        cartService.updateQuantity(cartItemId, request.quantity(), user);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Cart item quantity updated successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    //  CHECKOUT
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> checkout(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        cartService.checkout(user);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Order placed successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}