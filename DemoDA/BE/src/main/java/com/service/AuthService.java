package com.service;


import com.dto.auth.AuthResponse;
import com.dto.auth.LoginRequest;
import com.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}