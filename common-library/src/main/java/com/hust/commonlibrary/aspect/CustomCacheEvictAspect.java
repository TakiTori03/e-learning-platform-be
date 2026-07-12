package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.CustomCacheEvict;
import com.hust.commonlibrary.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class CustomCacheEvictAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @AfterReturning("@annotation(customCacheEvict)")
    public void evictCache(JoinPoint joinPoint, CustomCacheEvict customCacheEvict) {
        String cacheKey = null;

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

            Expression expression = expressionCache.computeIfAbsent(customCacheEvict.key(), parser::parseExpression);
            cacheKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("CustomCacheEvict Aspect SpEL Parsing Error", e);
            return;
        }

        if (cacheKey != null && !cacheKey.isBlank()) {
            // ðŸ’¡ TRÃ™NG KHá»šP: Pháº£i khá»›p tuyá»‡t Ä‘á»‘i cáº¥u trÃºc key vá»›i Aspect Cache Ä‘á»c
            String finalCacheKey = cacheKey;
            try {
                redisService.delete(finalCacheKey);
                log.debug("ðŸ§¹ CustomCacheEvict SUCCESS: Cleared key = {}", finalCacheKey);
            } catch (Exception e) {
                log.error("CustomCacheEvict Error during delete operation for key: {}", finalCacheKey, e);
            }
        }
    }
}

