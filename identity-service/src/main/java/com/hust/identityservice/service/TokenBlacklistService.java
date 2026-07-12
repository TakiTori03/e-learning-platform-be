package com.hust.identityservice.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenBlacklistService {
    void blacklistToken(String token, Jwt jwt);
    boolean isBlacklisted(String token);
}
