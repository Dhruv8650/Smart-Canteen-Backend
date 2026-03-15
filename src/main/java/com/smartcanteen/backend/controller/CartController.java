package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.request.AddToCartRequestDTO;
import com.smartcanteen.backend.dto.request.UpdateCartItemRequestDTO;
import com.smartcanteen.backend.dto.response.CartResponseDTO;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public String addToCart(@RequestBody AddToCartRequestDTO request,
                            @AuthenticationPrincipal UserDetails userDetails){
        User user=userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()-> new UserNotFoundException("User not found"));
        cartService.addToCart(request,user);

        return "Item added to cart successfully";
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public CartResponseDTO getCart(@AuthenticationPrincipal UserDetails userDetails){
        User user= userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        return  cartService.getCart(user);
    }

    @DeleteMapping("/item/{cartItemId}")
    @PreAuthorize("hasRole('USER')")
    public String removeItem(@PathVariable Long cartItemId,@AuthenticationPrincipal UserDetails userDetails){
        User user=userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        cartService.removeItem(cartItemId,user);

        return "Item removed form cart";
    }

    @PutMapping("/item/{cartItemId}")
    @PreAuthorize("hasRole('USER')")
    public String updateQuantity(
            @PathVariable Long cartItemId,
            @RequestBody UpdateCartItemRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        cartService.updateQuantity(cartItemId, request.quantity(), user);

        return "Cart item quantity updated";
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public String checkout(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        cartService.checkout(user);

        return "Order placed successfully";
    }
}
