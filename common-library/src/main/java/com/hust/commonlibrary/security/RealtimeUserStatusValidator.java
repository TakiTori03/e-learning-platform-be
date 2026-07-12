package com.hust.commonlibrary.security;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@RequiredArgsConstructor
@Slf4j
public class RealtimeUserStatusValidator implements OAuth2TokenValidator<Jwt> {

    private final RedisService redisService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String userId = jwt.getSubject();
        if (userId == null) {
            return OAuth2TokenValidatorResult.success();
        }

        String key = RedisPrefixConstants.getSharedUserBlacklistKey(userId);

        Object statusObj = redisService.get(key);
        if (statusObj != null) {
            String status = statusObj.toString();
            log.warn("🔒 Chặn truy cập! Người dùng {} có trạng thái blacklist: {}", userId, status);
            if ("LOCKED".equals(status) || "REJECTED".equals(status) || "DISABLED".equals(status)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("access_denied", "Tài khoản đã bị khóa hoặc từ chối bởi quản trị viên", null));
            } else if ("ROLE_CHANGED".equals(status)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("token_expired", "Quyền hạn thay đổi, vui lòng đăng nhập lại", null));
            }
        }
        return OAuth2TokenValidatorResult.success();
    }
}
