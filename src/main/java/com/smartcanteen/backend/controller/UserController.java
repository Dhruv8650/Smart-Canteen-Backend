package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.response.UserResponseDTO;
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
    public UserResponseDTO register(@RequestBody User user){

        User savedUser =userService.registerUser(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
                //savedUser.getRole()
        );
    }

    @PostMapping("/login")
    public UserResponseDTO login(@RequestBody User user){

        User loggedInUser= userService.login(user.getEmail(),user.getPassword());
        return new UserResponseDTO(
                loggedInUser.getId(),
                loggedInUser.getName(),
                loggedInUser.getEmail()
                //loggedInUser.getRole()
        );
    }
}
