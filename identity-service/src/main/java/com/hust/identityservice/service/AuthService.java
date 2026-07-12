package com.hust.identityservice.service;

import com.hust.identityservice.dto.request.ChangePasswordRequest;
import com.hust.identityservice.dto.request.InstructorRegistrationRequest;
import com.hust.identityservice.dto.request.LoginRequest;
import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    
    UserResponse login(LoginRequest request, HttpServletResponse response);
    
    UserResponse register(UserRegistrationRequest request);
    
    UserResponse registerInstructor(InstructorRegistrationRequest request);
    
    void logout(HttpServletRequest request, HttpServletResponse response);
    
    void refreshToken(HttpServletRequest request, HttpServletResponse response);
    
    void changePassword(ChangePasswordRequest request);
}
