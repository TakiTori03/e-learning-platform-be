package com.hust.identityservice.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.annotation.CustomCache;
import com.hust.commonlibrary.annotation.CustomCacheEvict;
import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.service.RedisService;
import com.hust.identityservice.dto.request.AdminUserCreationRequest;
import com.hust.identityservice.dto.request.UserUpdateRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import com.hust.identityservice.mapper.UserMapper;
import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.repository.UserSpecification;
import com.hust.identityservice.repository.http.AuthRepository;
import com.hust.identityservice.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RedisService redisService;

    private void syncSharedProfileToRedis(User user) {
        try {
            if (user == null || user.getId() == null) return;
            String redisKey = RedisPrefixConstants.getSharedUserProfileKey(user.getId().toString());
            UserSharedProfile sharedProfile = UserSharedProfile.builder()
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .email(user.getEmail())
                    .headline(user.getHeadline())
                    .biography(user.getBiography())
                    .build();
            redisService.set(redisKey, sharedProfile);
            log.info("Synced shared profile permanently to Redis for User {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to sync shared profile to Redis for User {}: {}", user.getId(), e.getMessage());
        }
    }


    @Override
    @Transactional
    @CustomCacheEvict(key = "'user:profile:' + #userId") // 🧹 DỌN DẸP CACHE: Xóa cache profile cũ khi gán Role!
    public void assignRole(String userId, String roleName) {
        // 1. Ghi lên Keycloak Server
        authRepository.assignRole(userId, roleName);

        // 2. Đồng bộ Ghi kép (Dual-write) xuống Local DB PostgreSQL
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setRole(roleName);
        userRepository.save(user);

        syncSharedProfileToRedis(user);

        log.info("BFF: Assigned role {} and synchronized DB for User {}", roleName, userId);
    }

    @Override
    public List<String> getAvailableRoles() {
        return authRepository.getAvailableRoles().stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    @Override
    public List<UserResponse> getUsersByStatus(String status) {
        UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
        List<User> users = userRepository.findByStatus(userStatus);

        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'user:profile:' + #userId")
    public void updateUserStatus(String userId, String status) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserStatus oldStatus = user.getStatus();
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
        user.setStatus(newStatus);

        userRepository.save(user);
        log.info("Admin updated user status: {} -> {}", userId, newStatus);

        // 🔥 LUỒNG DUYỆT GIẢNG VIÊN (Accept flow):
        if (oldStatus == UserStatus.PENDING && newStatus == UserStatus.ACTIVE) {
            log.info("Admin approved user {}. Granting INSTRUCTOR role in Keycloak and DB.", userId);

            // 1. Gán trên Keycloak
            authRepository.assignRole(userId, AppConstants.Role_Constants.ROLE_INSTRUCTOR);

            // 2. Đồng bộ Ghi kép xuống Local DB SQL
            user.setRole(AppConstants.Role_Constants.ROLE_INSTRUCTOR);
            userRepository.save(user);
        }

        // Đồng bộ hóa trạng thái lên Redis Blacklist dùng chung thời gian thực và Keycloak
        String blacklistKey = RedisPrefixConstants.getSharedUserBlacklistKey(userId);
        if (newStatus == UserStatus.LOCKED || newStatus == UserStatus.REJECTED) {
            redisService.set(blacklistKey, newStatus.name(), 900, TimeUnit.SECONDS);
            log.info("🔒 Đã đưa user {} vào Redis Blacklist với trạng thái {}", userId, newStatus.name());

            // Đồng bộ trạng thái KHÓA lên Keycloak
            authRepository.updateUserEnabled(userId, false);
        } else if (newStatus == UserStatus.ACTIVE) {
            redisService.delete(blacklistKey);
            log.info("🔓 Đã giải phóng user {} khỏi Redis Blacklist", userId);

            // Đồng bộ trạng thái KÍCH HOẠT lại lên Keycloak
            authRepository.updateUserEnabled(userId, true);
        }

        syncSharedProfileToRedis(user);
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'user:profile:' + #userId")
    public void approveInstructor(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        log.info("Admin approved instructor registration for user: {}", userId);

        // 1. Gán trên Keycloak và Enable Account
        authRepository.assignRole(userId, AppConstants.Role_Constants.ROLE_INSTRUCTOR);
        authRepository.updateUserEnabled(userId, true);

        // 2. Đồng bộ Ghi kép xuống Local DB SQL
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(AppConstants.Role_Constants.ROLE_INSTRUCTOR);
        userRepository.save(user);

        syncSharedProfileToRedis(user);
    }

    @Override
    @CustomCache(key = "'user:profile:' + #id", ttl = 24, unit = TimeUnit.HOURS)
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Override
    public List<UserResponse> getUsersByIds(List<String> ids) {
        List<UUID> uuids = ids.stream()
                .map(UUID::fromString)
                .toList();

        List<User> users = userRepository.findAllById(uuids);

        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateLastLogin(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setLastLogin(java.time.Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'user:profile:' + #userId")
    public UserResponse updateMyProfile(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean nameChanged = false;
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
            nameChanged = true;
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
            nameChanged = true;
        }
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getHeadline() != null) user.setHeadline(request.getHeadline());
        if (request.getBiography() != null) user.setBiography(request.getBiography());
        if (request.getSocials() != null) user.setSocials(request.getSocials());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());

        user = userRepository.save(user);

        // Đồng bộ Tên mới lên Keycloak để lần đăng nhập sau JWT Token không bị lấy lại tên cũ
        if (nameChanged) {
            authRepository.updateUserProfile(userId, user.getFirstName(), user.getLastName());
        }

        syncSharedProfileToRedis(user);

        // Trả về trực tiếp từ mapper (Không gọi Keycloak)
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(AdminUserCreationRequest request) {
        log.info("Admin creating new user with email: {}, role: {}", request.getEmail(), request.getRole());

        if (Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail()))) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // 1. Tạo user trên Keycloak
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(request.getEmail());
        userRep.setEmail(request.getEmail());
        userRep.setFirstName(request.getFirstName());
        userRep.setLastName(request.getLastName());
        userRep.setEnabled(true);
        userRep.setEmailVerified(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setTemporary(false);
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getPassword());
        userRep.setCredentials(List.of(cred));

        String keycloakUserId = authRepository.createUser(userRep);

        try {
            // 2. Gán Role trên Keycloak
            authRepository.assignRole(keycloakUserId, request.getRole());

            // 3. Ghi nhận Database local
            User user = User.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .role(request.getRole())
                    .status(UserStatus.ACTIVE)
                    .avatar(AppConstants.DEFAULT_AVATAR)
                    // .showProfile(true)
                    // .showCourses(true)
                    .build();
            user.setId(UUID.fromString(keycloakUserId));

            user = userRepository.save(user);
            log.info("BFF: User registered by Admin and synced to DB: {}", user.getEmail());

            syncSharedProfileToRedis(user);

            return userMapper.toUserResponse(user);
        } catch (Exception e) {
            log.error("💥 Dual-Write Error: Local database registration failed for admin created user: {}. Deleting user from Keycloak for consistency.", request.getEmail(), e);
            authRepository.deleteUser(keycloakUserId);
            throw e;
        }
    }

    @Override
    public List<UserResponse> getInstructorsSelect(String q) {
        log.info("Fetching instructors from DB for selection dropdown with query: {}", q);
        String searchKey = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        Specification<User> spec = Specification.where(UserSpecification.hasSearchQuery(searchKey))
                .and(UserSpecification.hasRole(AppConstants.Role_Constants.ROLE_INSTRUCTOR));
        List<User> instructors = userRepository.findAll(spec);
        return instructors.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<UserResponse> getAllUsers(Pageable pageable, String q, String role, UserStatus status) {
        log.info("Fetching paginated user list with filters - q: {}, role: {}, status: {}, pageable: {}", q, role, status, pageable);

        String searchKey = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        String roleFilter = (role != null && !role.trim().isEmpty()) ? role.trim() : null;

        Specification<User> spec = Specification.where(UserSpecification.hasSearchQuery(searchKey))
                .and(UserSpecification.hasRole(roleFilter))
                .and(UserSpecification.hasStatus(status));

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(userMapper::toUserResponse)
                .toList();

        return ListResponse.of(userResponses, userPage);
    }
}



