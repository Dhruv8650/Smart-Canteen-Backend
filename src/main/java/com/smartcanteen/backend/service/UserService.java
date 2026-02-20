package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.entity.User;

public interface UserService {
    User registerUser(RegisterRequestDTO request);

    User login(String email,String password);


}
