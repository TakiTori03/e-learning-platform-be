package com.hust.identityservice.service.impl;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.identityservice.dto.request.ChangePasswordRequest;
import com.hust.identityservice.dto.request.InstructorRegistrationRequest;
import com.hust.identityservice.dto.request.LoginRequest;
import com.hust.identityservice.dto.request.UserRegistrationRequest;
import com.hust.identityservice.dto.response.UserResponse;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import com.hust.identityservice.mapper.UserMapper;
import com.hust.identityservice.repository.UserRepository;
import com.hust.identityservice.repository.http.AuthRepository;
import com.hust.identityservice.service.AuthService;
import com.hust.identityservice.service.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;

    @org.springframework.beans.factory.annotation.Value("${app.cookie-domain:}")
    private String cookieDomain;

    @org.springframework.beans.factory.annotation.Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    @Transactional
    public UserResponse login(LoginRequest request, HttpServletResponse response) {
        log.info("Processing login for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVED);
        }

        AccessTokenResponse tokenResponse = authRepository.login(request.getEmail(), request.getPassword());
        setTokenCookies(response, tokenResponse);

        // Cập nhật thời gian đăng nhập cuối
        user.setLastLogin(java.time.Instant.now());
        userRepository.save(user);

        // 🔥 TỐI ƯU: Dùng thẳng DB Roles đã được đồng bộ thay vì gọi Keycloak
        log.info("BFF: User logged in and cookies set for: {}", user.getEmail());
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        log.info("Registering new student applicant: {}", request.getEmail());
        String keycloakUserId = createKeycloakUser(request, true);

        try {
            // 1. Gán Role Student trên Keycloak
            authRepository.assignRole(keycloakUserId, AppConstants.Role_Constants.ROLE_STUDENT);

            // 2. Ghi nhận Database local
            User user = userMapper.toUser(request);
            user.setId(UUID.fromString(keycloakUserId));
            user.setAvatar(AppConstants.DEFAULT_AVATAR);
            user.setStatus(UserStatus.ACTIVE);

            // 🔥 ĐỒNG BỘ GHI KÉP (Dual-Write Sync)
            user.setRole(AppConstants.Role_Constants.ROLE_STUDENT);

            user = userRepository.save(user);
            log.info("BFF: Student registered and synced to DB with standard roles: {}", user.getEmail());

            return userMapper.toUserResponse(user);
        } catch (Exception e) {
            log.error("💥 Dual-Write Error: Local database registration failed for student email: {}. Deleting user from Keycloak for consistency.", request.getEmail(), e);
            authRepository.deleteUser(keycloakUserId);
            throw e;
        }
    }

    @Override
    @Transactional
    public UserResponse registerInstructor(InstructorRegistrationRequest request) {
        log.info("Registering new Instructor applicant: {}", request.getEmail());
        String keycloakUserId = createKeycloakUser(request, false);

        try {
            // QUAN TRỌNG: KHÔNG gán Role Instructor tại đây trên Keycloak (Đợi Admin duyệt)
            User user = userMapper.toUser(request);
            user.setId(UUID.fromString(keycloakUserId));
            user.setAvatar(AppConstants.DEFAULT_AVATAR);
            user.setStatus(UserStatus.PENDING); // Đăng ký mới chờ duyệt
            user.setRole(AppConstants.Role_Constants.ROLE_INSTRUCTOR); // Thiết lập vai trò dự kiến trong DB

            user = userRepository.save(user);
            log.info("BFF: Instructor candidate registered and pending verification: {}", user.getEmail());

            return userMapper.toUserResponse(user);
        } catch (Exception e) {
            log.error("💥 Dual-Write Error: Local database registration failed for instructor email: {}. Deleting user from Keycloak for consistency.", request.getEmail(), e);
            authRepository.deleteUser(keycloakUserId);
            throw e;
        }
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, AppConstants.Token_Constants.REFRESH_TOKEN);

        if (refreshToken != null && !refreshToken.isEmpty()) {
            authRepository.logout(refreshToken);
        }

        com.hust.commonlibrary.utils.SecurityUtils.getCurrentJwt().ifPresent(jwt -> {
            String accessToken = extractCookie(request, AppConstants.Token_Constants.ACCESS_TOKEN);
            tokenBlacklistService.blacklistToken(accessToken, jwt);
        });

        clearCookie(response, AppConstants.Token_Constants.ACCESS_TOKEN);
        clearCookie(response, AppConstants.Token_Constants.REFRESH_TOKEN);

        log.info("BFF: Tokens cleared and blacklisted. Logout complete.");
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, AppConstants.Token_Constants.REFRESH_TOKEN);

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        AccessTokenResponse tokenResponse = authRepository.refreshToken(refreshToken);
        setTokenCookies(response, tokenResponse);
        log.debug("BFF: Successfully refreshed token rotation cycle.");
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Request password change for userId: {}", userId);

        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
            // Verify old password by logging in
            authRepository.login(user.getEmail(), request.getOldPassword());
        } catch (Exception e) {
            throw new AppException(ErrorCode.EMAIL_PASSWORD_NOT_MATCH);
        }

        authRepository.resetPassword(userId, request.getNewPassword());
        log.info("BFF: Password updated securely on Keycloak for userId: {}", userId);
    }

    // --- PRIVATE CONVENIENCE HELPERS ---

    private String createKeycloakUser(UserRegistrationRequest request, boolean isEnabled) {
        if (Boolean.TRUE.equals(userRepository.existsByEmail(request.getEmail()))) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(request.getEmail());
        userRep.setEmail(request.getEmail());
        userRep.setFirstName(request.getFirstName());
        userRep.setLastName(request.getLastName());
        userRep.setEnabled(isEnabled);
        userRep.setEmailVerified(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setTemporary(false);
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getPassword());
        userRep.setCredentials(List.of(cred));

        return authRepository.createUser(userRep);
    }

    private void setTokenCookies(HttpServletResponse response, AccessTokenResponse tokenResponse) {
        ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from(AppConstants.Token_Constants.ACCESS_TOKEN, tokenResponse.getToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(tokenResponse.getExpiresIn())
                .sameSite("Lax");

        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            accessCookieBuilder.domain(cookieDomain);
        }

        ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from(AppConstants.Token_Constants.REFRESH_TOKEN, tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(tokenResponse.getRefreshExpiresIn() > 0 ? tokenResponse.getRefreshExpiresIn() : 604800)
                .sameSite("Lax");

        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            refreshCookieBuilder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookieBuilder.build().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieBuilder.build().toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0);

        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookieBuilder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
