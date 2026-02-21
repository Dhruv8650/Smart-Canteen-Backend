package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.EmailAlreadyExistException;
import com.smartcanteen.backend.exception.InvalidCredentialsException;
import com.smartcanteen.backend.exception.UserNotFoundException;
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
    public User registerUser(RegisterRequestDTO request){

        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new EmailAlreadyExistException("Email already exist");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(("User not found")));

        if(!passwordEncoder.matches(password,user.getPassword())){
            throw new InvalidCredentialsException("Invalid password");
        }
        return user;
    }

    @Override
    public User createManager(String name, String email, String password) {
        if(userRepository.findByEmail(email).isPresent()){
            throw new RuntimeException("Email Already exist");
        }
        User manager=new User();
        manager.setName(name);
        manager.setEmail(email);
        manager.setPassword(passwordEncoder.encode(password));
        manager.setRole(Role.MANAGER);

        return userRepository.save(manager);
    }


}
