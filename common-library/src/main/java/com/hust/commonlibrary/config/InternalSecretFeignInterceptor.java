package com.hust.commonlibrary.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.AppConstants;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign Interceptor: Tự động chèn mã bí mật nội bộ cho tất cả cuộc gọi Feign
 * để vượt qua InternalApiSecurityFilter của các service.
 */
@Component
@Slf4j
@ConditionalOnClass(RequestInterceptor.class)
public class InternalSecretFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        log.debug("Feign Interceptor: Injecting Internal Secret");
        template.header(AppConstants.INTERNAL_SECRET_HEADER, AppConstants.INTERNAL_SECRET_VALUE);
    }
}
