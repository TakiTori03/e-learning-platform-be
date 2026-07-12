package com.hust.identityservice.security;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.identityservice.entity.User;
import com.hust.identityservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:https://app.hust-elearning.online}")
    private String frontendUrl;

    @Value("${app.cookie-domain:}")
    private String cookieDomain;

    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());

        String accessToken = client.getAccessToken().getTokenValue();
        String refreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        syncUserToDatabase(oidcUser);

        // Đổ Token vào Cookie trả về cho Frontend
        assert client.getAccessToken().getExpiresAt() != null;
        ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from(AppConstants.Token_Constants.ACCESS_TOKEN, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(client.getAccessToken().getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond())
                .sameSite("Lax");

        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            accessCookieBuilder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookieBuilder.build().toString());

        if (refreshToken != null) {
            ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from(AppConstants.Token_Constants.REFRESH_TOKEN, refreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60) // 7 days
                    .sameSite("Lax");

            if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
                refreshCookieBuilder.domain(cookieDomain);
            }
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieBuilder.build().toString());
        }

        // Đổ ID Token vào Cookie để dùng cho luồng Logout (id_token_hint)
        String idToken = oidcUser.getIdToken().getTokenValue();
        ResponseCookie.ResponseCookieBuilder idCookieBuilder = ResponseCookie.from(AppConstants.Token_Constants.ID_TOKEN, idToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days (matches refresh token)
                .sameSite("Lax");

        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            idCookieBuilder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, idCookieBuilder.build().toString());

        log.info("User {} logged in successfully via OAuth2. Redirecting to Frontend...", oidcUser.getEmail());

        // Redirect về Frontend sau khi xử lý xong
        response.sendRedirect(frontendUrl + "/auth-callback");
    }

    private void syncUserToDatabase(OidcUser oidcUser) {
        String sub = oidcUser.getSubject(); // Keycloak User ID (UUID)
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();

        User user = userRepository.findById(UUID.fromString(sub))
                .map(existingUser -> {
                    // Cập nhật thông tin nếu có thay đổi trên Keycloak
                    existingUser.setFirstName(firstName);
                    existingUser.setLastName(lastName);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    log.info("Tạo mới user từ Keycloak vào Local DB: {}", email);
                    User newUser = User.builder()
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .phone("") // Default empty for new social users
                            .build();

                    newUser.setId(UUID.fromString(sub));
                    return userRepository.save(newUser);
                });

        log.info("Đã đồng bộ thông tin User {} từ Keycloak", user.getEmail());
    }
}
