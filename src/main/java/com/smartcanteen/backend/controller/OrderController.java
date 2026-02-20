package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.service.OrderService;
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
    public Order placeOrder(@RequestBody List<Long> foodIds, Authentication authentication){
        String email=authentication.getName();

        return orderService.placeOrder(foodIds,email);
    }

    // USER SEE OWN ORDERS
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public List<Order> getMyOrders(Authentication authentication){
        String email=authentication.getName();
        return orderService.getUserOrder(email);
    }

    // ADMIN SEE ALL USER
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> getAllOrders(){
        return orderService.getAllOrders();
    }
}
