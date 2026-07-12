package com.hust.commonlibrary.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistValidator implements OAuth2TokenValidator<Jwt> {

    private final RedisService redisService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenValue = jwt.getTokenValue();
        if (tokenValue == null) {
            return OAuth2TokenValidatorResult.success();
        }

        String key = RedisPrefixConstants.getSharedTokenBlacklistKey(tokenValue);

        if (redisService.hasKey(key)) {
            log.warn("🔒 Chặn truy cập bằng Access Token đã bị thu hồi (Logout)!");
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token đã bị thu hồi", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
