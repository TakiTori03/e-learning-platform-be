package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.TrackView;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class TrackViewAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final java.util.Map<String, Expression> expressionCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Around("@annotation(trackView)")
    public Object track(ProceedingJoinPoint joinPoint, TrackView trackView) throws Throwable {
        String type = trackView.type();
        String spelExpression = trackView.value();

        // 1. Parse target ID using SpEL
        String id = null;
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = signature.getParameterNames();

            EvaluationContext context = new StandardEvaluationContext();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            if (!spelExpression.isBlank()) {
                Expression expression = expressionCache.computeIfAbsent(spelExpression, parser::parseExpression);
                id = expression.getValue(context, String.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse SpEL expression '{}' in TrackViewAspect: {}", spelExpression, e.getMessage());
        }

        if (id != null && !id.trim().isEmpty()) {
            // 2. Resolve client IP and user ID
            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            String ip = getClientIp();

            // 3. Construct Redis lock key
            String lockKey = null;
            if (userId != null && !userId.trim().isEmpty() && !userId.equals("anonymousUser")) {
                lockKey = type + ":views:lock:" + id + ":" + userId;
            } else if (ip != null && !ip.trim().isEmpty()) {
                lockKey = type + ":views:lock:" + id + ":" + ip;
            }

            boolean canIncrement = true;
            if (lockKey != null) {
                // Set lock with 1 hour TTL (anti-spam)
                canIncrement = redisService.setIfAbsent(lockKey, "1", 1, TimeUnit.HOURS);
            }

            if (canIncrement) {
                String countKey = type + ":views:count:" + id;
                redisService.increment(countKey, 1);
                log.info("Aspect incremented view count in Redis for {}: {}, lockKey: {}", type, id, lockKey);
            } else {
                log.debug("Aspect view increment ignored due to lock: {}: {}, lockKey: {}", type, id, lockKey);
            }
        }

        return joinPoint.proceed();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception e) {
            log.warn("Cannot extract client IP in TrackViewAspect: {}", e.getMessage());
        }
        return null;
    }
}

