package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.RateLimit;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class RateLimitAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final java.util.Map<String, Expression> expressionCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        String targetKey = resolveRateLimitKey(joinPoint, rateLimit);
        String finalKey = "elearning:ratelimit:" + targetKey;

        // Thá»±c hiá»‡n tÄƒng nguyÃªn tá»­ vÃ  tá»± Ä‘á»™ng thiáº¿t láº­p TTL báº±ng Lua Script Ä‘á»ƒ chá»‘ng rÃ² rá»‰ khÃ³a vÄ©nh viá»…n
        Long currentCount;
        try {
            currentCount = redisService.incrementAndExpire(finalKey, rateLimit.period());
        } catch (Exception e) {
            log.error("âš ï¸ Redis Connection Error in RateLimitAspect on key: {}. Bypassing rate limit check.", finalKey, e);
            // Redis sáº­p -> Cho phÃ©p request Ä‘i qua Ä‘á»ƒ Ä‘áº£m báº£o tÃ­nh sáºµn sÃ ng (High Availability)
            return;
        }

        // Kiá»ƒm tra vÆ°á»£t ngÆ°á»¡ng
        if (currentCount != null && currentCount > rateLimit.limit()) {
            log.warn("RateLimit Triggered on key: {}. Limit: {}/{}s. Current: {}", 
                     finalKey, rateLimit.limit(), rateLimit.period(), currentCount);
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        log.debug("RateLimit Tracked on key: {}. Count: {}/{}", finalKey, currentCount, rateLimit.limit());
    }

    private String resolveRateLimitKey(JoinPoint joinPoint, RateLimit rateLimit) {
        // Æ¯U TIÃŠN 1: Giáº£i mÃ£ SpEL náº¿u Ä‘Æ°á»£c cung cáº¥p
        if (!rateLimit.key().isBlank()) {
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

                Expression expression = expressionCache.computeIfAbsent(rateLimit.key(), parser::parseExpression);
                String key = expression.getValue(context, String.class);
                if (key != null && !key.isBlank()) {
                    return "custom:" + key;
                }
            } catch (Exception e) {
                log.error("RateLimit Aspect SpEL Parsing Error", e);
            }
        }

        // Æ¯U TIÃŠN 2: Láº¥y User ID tá»« SecurityContext náº¿u cÃ³
        try {
            String userId = SecurityUtils.getCurrentUserIdOrThrow();
            if (userId != null && !userId.isBlank()) {
                return "user:" + userId;
            }
        } catch (Exception e) {
            // Bá» qua náº¿u user chÆ°a Ä‘Äƒng nháº­p
        }

        // Æ¯U TIÃŠN 3: Tá»± Ä‘á»™ng láº¥y Ä‘á»‹a chá»‰ Client IP lÃ m chÃ¬a khÃ³a dá»± phÃ²ng
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);
            return "ip:" + ip;
        }

        return "global:default";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // TrÆ°á»ng há»£p cháº¡y qua Load Balancer, láº¥y IP Ä‘áº§u tiÃªn
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

