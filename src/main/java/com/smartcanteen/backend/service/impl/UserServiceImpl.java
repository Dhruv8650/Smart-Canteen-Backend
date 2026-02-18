package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository){
        this.userRepository=userRepository;
    }
    @Override
    public User registerUser(User user){

        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new RuntimeException("Email already exixt");
        }
        return userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException(("User not found")));

        if(!user.getPassword().equals(password)){
            throw new RuntimeException(("Invalid password"));
        }
        return user;
    }
}
