package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.User;

public interface UserService {
    User registerUser(User user);

    User login(String email,String password);
}
