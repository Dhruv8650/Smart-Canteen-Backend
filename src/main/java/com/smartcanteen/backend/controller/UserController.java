package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController (UserService userService){
        this.userService=userService;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user){
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public User login(@RequestBody User user){
        return userService.login(user.getEmail(),user.getPassword());
    }
}
