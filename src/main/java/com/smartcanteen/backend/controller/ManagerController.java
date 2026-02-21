package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.util.List;

@RestController
@RequestMapping("/manager/orers")
public class ManagerController {

    private final OrderService orderService;

    public ManagerController(OrderService orderService){
        this.orderService=orderService;
    }

    @PutMapping("/{orderId/status}")
    @PreAuthorize("hasRole('MANAGER')")
    public OrderResponseDTO updateStatus(@PathVariable Long orderId,
                                         @RequestBody OrderStatus status){
        return orderService.updateOrderStatus(orderId,status);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public List<OrderResponseDTO> getPendingOrders(){
        return orderService.getPendingOrders();
    }
}
