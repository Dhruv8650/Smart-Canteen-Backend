package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.security.PublicKey;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService=orderService;
    }

    // USER PLACES ORDER
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public OrderResponseDTO placeOrder(@Valid @RequestBody OrderRequestDTO request, Authentication authentication){
        String email=authentication.getName();

        return orderService.placeOrder(request,authentication.getName());
    }

    // USER SEE OWN ORDERS
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public List<OrderResponseDTO> getMyOrders(Authentication authentication){
        String email=authentication.getName();
        return orderService.getUserOrder(authentication.getName());
    }

    // ADMIN SEE ALL USER
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponseDTO> getAllOrders(){
        return orderService.getAllOrders();
    }
}
