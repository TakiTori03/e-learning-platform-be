package com.hust.identityservice.security;

import com.hust.identityservice.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@RequiredArgsConstructor
@Slf4j
public class RedisBlacklistJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenValue = jwt.getTokenValue();

        // Check Redis Blacklist
        if (tokenBlacklistService.isBlacklisted(tokenValue)) {
            log.warn("Chặn truy cập bằng Access Token đã bị thu hồi trong Redis Blacklist!");
            OAuth2Error error = new OAuth2Error("invalid_token", "The token has been revoked", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
