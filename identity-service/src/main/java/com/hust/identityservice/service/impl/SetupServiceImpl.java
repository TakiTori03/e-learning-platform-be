package com.hust.identityservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.service.RedisService;
import com.hust.identityservice.config.KeycloakConfig;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.repository.http.AuthRepository;
import com.hust.identityservice.service.SetupService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupServiceImpl implements SetupService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final Keycloak keycloakAdminClient;
    private final KeycloakConfig keycloakConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void seedUsers() {
        log.info("Starting mock users seeding process...");
        try {
            // 1. Read mock users from JSON file
            InputStream inputStream = new ClassPathResource("data/mock-users.json").getInputStream();
            List<MockUserDto> mockUsers = objectMapper.readValue(inputStream, new TypeReference<List<MockUserDto>>() {});

            for (MockUserDto mockUser : mockUsers) {
                // Check if user already exists in local DB
                if (userRepository.existsByEmail(mockUser.getEmail())) {
                    log.info("User {} already exists in PostgreSQL. Skipping DB insertion.", mockUser.getEmail());
                    // Repair Redis cache if missing
                    userRepository.findByEmail(mockUser.getEmail()).ifPresent(this::syncSharedProfileToRedis);
                    continue;
                }

                String keycloakUserId = null;
                try {
                    // Create user on Keycloak
                    UserRepresentation userRep = new UserRepresentation();
                    userRep.setUsername(mockUser.getEmail());
                    userRep.setEmail(mockUser.getEmail());
                    userRep.setFirstName(mockUser.getFirstName());
                    userRep.setLastName(mockUser.getLastName());
                    userRep.setEnabled(true);
                    userRep.setEmailVerified(true);

                    CredentialRepresentation cred = new CredentialRepresentation();
                    cred.setTemporary(false);
                    cred.setType(CredentialRepresentation.PASSWORD);
                    cred.setValue(mockUser.getPassword());
                    userRep.setCredentials(List.of(cred));

                    keycloakUserId = authRepository.createUser(userRep);
                    log.info("Created user {} on Keycloak with ID: {}", mockUser.getEmail(), keycloakUserId);
                } catch (AppException e) {
                    if (e.getErrorCode() == ErrorCode.KEYCLOAK_USER_CONFLICT) {
                        log.info("User {} already exists on Keycloak. Fetching ID...", mockUser.getEmail());
                        List<UserRepresentation> existing = keycloakAdminClient.realm(keycloakConfig.getRealm())
                                .users()
                                .search(mockUser.getEmail());
                        if (existing != null && !existing.isEmpty()) {
                            keycloakUserId = existing.get(0).getId();
                        } else {
                            throw new AppException(ErrorCode.KEYCLOAK_ERROR);
                        }
                    } else {
                        throw e;
                    }
                }

                if (keycloakUserId == null) {
                    throw new AppException(ErrorCode.KEYCLOAK_ERROR);
                }

                // 2. Assign Role on Keycloak
                String roleName = mockUser.getRole();
                if ("ADMIN".equalsIgnoreCase(roleName)) {
                    authRepository.assignRole(keycloakUserId, AppConstants.Role_Constants.ROLE_ADMIN);
                } else if ("INSTRUCTOR".equalsIgnoreCase(roleName)) {
                    authRepository.assignRole(keycloakUserId, AppConstants.Role_Constants.ROLE_INSTRUCTOR);
                } else {
                    authRepository.assignRole(keycloakUserId, AppConstants.Role_Constants.ROLE_STUDENT);
                }

                // 3. Save to PostgreSQL local DB
                User user = User.builder()
                        .email(mockUser.getEmail())
                        .firstName(mockUser.getFirstName())
                        .lastName(mockUser.getLastName())
                        .phone(mockUser.getPhone())
                        .address(mockUser.getAddress())
                        .avatar(mockUser.getAvatar() != null ? mockUser.getAvatar() : AppConstants.DEFAULT_AVATAR)
                        .headline(mockUser.getHeadline())
                        .biography(mockUser.getBiography())
                        .role("ADMIN".equalsIgnoreCase(roleName) ? AppConstants.Role_Constants.ROLE_ADMIN :
                              "INSTRUCTOR".equalsIgnoreCase(roleName) ? AppConstants.Role_Constants.ROLE_INSTRUCTOR :
                              AppConstants.Role_Constants.ROLE_STUDENT)
                        .status(UserStatus.ACTIVE)
                        .build();
                user.setId(UUID.fromString(keycloakUserId));

                user = userRepository.save(user);
                log.info("Saved user {} to PostgreSQL", user.getEmail());

                // 4. Sync Shared Profile to Redis permanently
                syncSharedProfileToRedis(user);
            }
            log.info("Mock users seeding process completed successfully!");
        } catch (Exception e) {
            log.error("Error occurred during mock users seeding: {}", e.getMessage(), e);
            throw new RuntimeException("Seeding failed: " + e.getMessage(), e);
        }
    }

    private void syncSharedProfileToRedis(User user) {
        try {
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

    @Data
    private static class MockUserDto {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String role;
        private String phone;
        private String address;
        private String headline;
        private String biography;
        private String avatar;
    }
}
