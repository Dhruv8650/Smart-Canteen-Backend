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
        return userRepository.save(user);
    }
}
