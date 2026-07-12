package com.hust.identityservice.service;


import com.hust.identityservice.dto.request.*;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.identityservice.entity.UserStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {


    
    void assignRole(String userId, String roleName);
    
    List<String> getAvailableRoles();
    
    List<UserResponse> getUsersByStatus(String status);
    
    void updateUserStatus(String userId, String status);
    
    void approveInstructor(String userId);
    
    UserResponse getUserById(String id);
    
    List<UserResponse> getUsersByIds(List<String> ids);
    
    void updateLastLogin(String userId);
    
    UserResponse updateMyProfile(String userId, UserUpdateRequest request);

    List<UserResponse> getInstructorsSelect(String q);
    
    UserResponse createUser(AdminUserCreationRequest request);
    
    ListResponse<UserResponse> getAllUsers(Pageable pageable, String q, String role, UserStatus status);
}


