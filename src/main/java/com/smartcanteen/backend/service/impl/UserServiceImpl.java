package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;

    }

    @Override
    public User registerUser(User user){

        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new RuntimeException("Email already exist");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException(("User not found")));

        if(!passwordEncoder.matches(password,user.getPassword())){
            throw new RuntimeException(("Invalid password"));
        }
        return user;
    }
}
