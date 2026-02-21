package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.request.CreateManagerRequestDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.service.UserService;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService){
        this.userService=userService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(){
        return "Welcome Admin!";
    }

    @PostMapping("/create-manager")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO createManager(@RequestBody CreateManagerRequestDTO request){
        User manager=userService.createManager(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        return new UserResponseDTO(
                manager.getId(),
                manager.getName(),
                manager.getEmail(),
                manager.getRole()
        );
    }
}
