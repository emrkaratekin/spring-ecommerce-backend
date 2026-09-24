package com.ecommerce.service;

import com.ecommerce.dto.response.UserResponse;

public interface UserService {

    UserResponse getCurrentUser(String email);
}
