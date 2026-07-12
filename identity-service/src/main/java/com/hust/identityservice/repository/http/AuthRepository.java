package com.hust.identityservice.repository.http;

import java.util.Collections;
import java.util.List;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.identityservice.config.KeycloakConfig;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AuthRepository {

    private final Keycloak keycloakAdminClient;
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate = new RestTemplate();


    /**
     * Luồng Login: Xác thực bằng Email và Password trực tiếp (Password Grant Type).
     */
    public AccessTokenResponse login(String email, String password) {
        MultiValueMap<String, String> body = createBaseForm();
        body.add("grant_type", "password");
        body.add("username", email);
        body.add("password", password);

        return executeTokenRequest(body);
    }

    /**
     * Luồng Refresh Token: Cấp mới Access Token từ Refresh Token cũ.
     */
    public AccessTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = createBaseForm();
        body.add("grant_type", AppConstants.Token_Constants.REFRESH_TOKEN);
        body.add("refresh_token", refreshToken);

        return executeTokenRequest(body);
    }

    /**
     * Luồng Logout (Revoke token trên Keycloak)
     */
    public void logout(String refreshToken) {
        String logoutEndpoint = keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> body = createBaseForm();
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, createHeaders());

        try {
            restTemplate.postForEntity(logoutEndpoint, request, String.class);
        } catch (Exception e) {
            log.warn("Lỗi khi revoke token trên Keycloak, có thể token đã bị hủy hoặc hết hạn: {}", e.getMessage());
        }
    }

    /**
     * Luồng Register: Dùng Admin Client tạo user mới.
     */
    public String createUser(UserRepresentation userRep) {
        try (Response response = keycloakAdminClient
                .realm(keycloakConfig.getRealm())
                .users()
                .create(userRep)) {

            int status = response.getStatus();

            if (status == Response.Status.CREATED.getStatusCode()) {
                return CreatedResponseUtil.getCreatedId(response);
            }

            // 409 Conflict: Thường là Email đã tồn tại
            if (status == Response.Status.CONFLICT.getStatusCode()) {
                log.warn("Keycloak Conflict: Đã tồn tại user với email: {}", userRep.getEmail());
                throw new AppException(ErrorCode.KEYCLOAK_USER_CONFLICT);
            }

            // 400 Bad Request: Dữ liệu format không đúng hoặc thiếu
            if (status == Response.Status.BAD_REQUEST.getStatusCode()) {
                String errorInfo = response.readEntity(String.class);
                log.error("Keycloak Creation Failed (400): {}", errorInfo);
                throw new AppException(ErrorCode.USERNAME_INVALID);
            }

            throw new AppException(ErrorCode.KEYCLOAK_ERROR);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi kết nối tới Keycloak Server: {}", e.getMessage());
            throw new AppException(ErrorCode.KEYCLOAK_ERROR);
        }
    }

    /**
     * Cập nhật mật khẩu cho User
     */
    public void resetPassword(String userId, String newPassword) {
        try {
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setTemporary(false);
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(newPassword);

            keycloakAdminClient.realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .resetPassword(cred);
            log.info("BFF: Reset password for user {}", userId);
        } catch (Exception e) {
            log.error("Lỗi khi reset password trên Keycloak: {}", e.getMessage());
            throw new AppException(ErrorCode.KEYCLOAK_ERROR);
        }
    }

    /**
     * Gán Role cho User (Realm level)
     */
    public void assignRole(String userId, String roleName) {
        try {
            RoleRepresentation roleRep = keycloakAdminClient
                    .realm(keycloakConfig.getRealm())
                    .roles()
                    .get(roleName)
                    .toRepresentation();

            keycloakAdminClient
                    .realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(Collections.singletonList(roleRep));
            log.info("BFF: Assigned role {} to user {}", roleName, userId);
        } catch (Exception e) {
            log.error("Lỗi khi gán role: {}", e.getMessage());
            throw new AppException(ErrorCode.KEYCLOAK_ROLE_NOT_FOUND);
        }
    }

    /**
     * Xóa User khỏi Keycloak Realm (Sử dụng cho giao dịch bù khi ghi kép cục bộ thất bại)
     */
    public void deleteUser(String userId) {
        try {
            keycloakAdminClient.realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .remove();
            log.warn("🧹 Rolled back / deleted Keycloak user {} due to local DB sync failure.", userId);
        } catch (Exception e) {
            log.error("💥 Critical: Failed to delete user {} from Keycloak during rollback: {}", userId, e.getMessage());
        }
    }

    /**
     * Kích hoạt hoặc vô hiệu hóa tài khoản của User trên Keycloak.
     */
    public void updateUserEnabled(String userId, boolean enabled) {
        try {
            UserRepresentation userRep = keycloakAdminClient.realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .toRepresentation();
            userRep.setEnabled(enabled);

            keycloakAdminClient.realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .update(userRep);
            log.info("BFF: Updated user enabled status to {} on Keycloak for userId: {}", enabled, userId);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái enabled trên Keycloak cho user {}: {}", userId, e.getMessage());
            throw new AppException(ErrorCode.KEYCLOAK_ERROR);
        }
    }

    /**
     * Cập nhật thông tin Profile cơ bản trên Keycloak (firstName, lastName).
     * Để lần đăng nhập sau token cấp ra chứa thông tin mới nhất.
     */
    public void updateUserProfile(String userId, String firstName, String lastName) {
        try {
            UserRepresentation userRep = keycloakAdminClient.realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .toRepresentation();

            boolean changed = false;
            if (firstName != null && !firstName.isBlank()) {
                userRep.setFirstName(firstName);
                changed = true;
            }
            if (lastName != null && !lastName.isBlank()) {
                userRep.setLastName(lastName);
                changed = true;
            }

            if (changed) {
                keycloakAdminClient.realm(keycloakConfig.getRealm())
                        .users()
                        .get(userId)
                        .update(userRep);
                log.info("BFF: Updated user profile (firstName, lastName) on Keycloak for userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật profile (firstName, lastName) trên Keycloak cho user {}: {}", userId, e.getMessage());
            // Có thể bỏ qua lỗi này nếu muốn local DB vẫn được cập nhật thành công, 
            // nhưng an toàn nhất là vẫn log lỗi để track.
        }
    }

    /**
     * Lấy danh sách các Role hiện có trong Realm
     */
    public List<RoleRepresentation> getAvailableRoles() {
        try {
            return keycloakAdminClient
                    .realm(keycloakConfig.getRealm())
                    .roles()
                    .list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách role từ Keycloak: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Lấy danh sách tên Role của một User
     */
    public java.util.Set<String> getRolesForUser(String userId) {
        try {
            return keycloakAdminClient
                    .realm(keycloakConfig.getRealm())
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("Lỗi khi lấy role của user {} từ Keycloak: {}", userId, e.getMessage());
            return java.util.Collections.emptySet();
        }
    }


    // --- HELPER METHODS ---

    public AccessTokenResponse executeTokenRequest(MultiValueMap<String, String> body) {
        String tokenEndpoint = keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm() + "/protocol/openid-connect/token";
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, createHeaders());

        try {
            ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(tokenEndpoint, request, AccessTokenResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            String errorResponse = e.getResponseBodyAsString();
            log.error("Keycloak Login Error: Status {}, Body {}", e.getStatusCode(), errorResponse);

            // Nếu sai credentials
            if (errorResponse.contains("invalid_grant")) {
                throw new AppException(ErrorCode.EMAIL_PASSWORD_NOT_MATCH);
            }

            throw new AppException(ErrorCode.UNAUTHENTICATED);
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gọi Keycloak Token: {}", e.getMessage());
            throw new AppException(ErrorCode.KEYCLOAK_ERROR);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private MultiValueMap<String, String> createBaseForm() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", keycloakConfig.getClientId());
        map.add("client_secret", keycloakConfig.getClientSecret());
        return map;
    }
}
