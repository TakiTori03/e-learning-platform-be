package com.hust.commonlibrary.security;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hust.commonlibrary.constants.AppConstants;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Security Filter for S2S (Service-to-Service) Communication.
 * Validates that incoming requests to "/internal/**" endpoints possess the correct custom secret token header,
 * preventing unauthorized lateral movement within the cluster even if endpoints are permitAll() in Spring Security.
 */
@Component
@Slf4j
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InternalApiSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.contains("/internal/")) {
            String secret = request.getHeader(AppConstants.INTERNAL_SECRET_HEADER);
            if (secret == null || !secret.equals(AppConstants.INTERNAL_SECRET_VALUE)) {
                log.warn("⚠️ Zero Trust Alert: Unauthorized S2S request attempted on path: {} from IP: {}. Missing or invalid X-Internal-Secret.",
                        path, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"401 Unauthorized: Invalid service-to-service credentials\",\"payload\":null}");
                return;
            }
            log.debug("🔑 Zero Trust: S2S request on path: {} authorized successfully via X-Internal-Secret.", path);
        }

        filterChain.doFilter(request, response);
    }
}
