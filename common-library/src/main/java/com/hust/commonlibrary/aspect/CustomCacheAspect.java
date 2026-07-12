package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.CustomCache;
import com.hust.commonlibrary.service.RedisService;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class CustomCacheAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Around("@annotation(customCache)")
    public Object handleCache(ProceedingJoinPoint joinPoint, CustomCache customCache) throws Throwable {
        String cacheKey = null;

        // 1. TrÃ­ch xuáº¥t Cache Key qua SpEL
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

            Expression expression = expressionCache.computeIfAbsent(customCache.key(), parser::parseExpression);
            cacheKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("CustomCache Aspect SpEL Parsing Error", e);
            // Lá»—i parse SpEL -> Bá» qua Cache, cháº¡y DB gá»‘c cho an toÃ n
            return joinPoint.proceed();
        }

        // Náº¿u SpEL parse ra null -> Bá» qua Cache, cháº¡y DB gá»‘c
        if (cacheKey == null || cacheKey.isBlank()) {
            return joinPoint.proceed();
        }

        // ðŸ’¡ KIÃŠN Cá»: Sá»­ dá»¥ng trá»±c tiáº¿p key Ä‘Ã£ qua phÃ¢n giáº£i SpEL. 
        // Káº¿t há»£p vá»›i prefix Microservice cá»§a RedisService sáº½ cho cáº¥u trÃºc cá»±c gá»n gÃ ng!
        String finalCacheKey = cacheKey;

        // 2. Thá»­ Ä‘á»c dá»¯ liá»‡u tá»« Cache (Bá»c try-catch phÃ²ng vá»‡ sáº­p Redis)
        try {
            Object cachedValue = redisService.get(finalCacheKey);
            if (cachedValue != null) {
                log.debug("ðŸš€ CustomCache HIT: key = {}", finalCacheKey);
                return cachedValue;
            }
        } catch (Exception e) {
            log.error("âš ï¸ Redis Connection Failure (Read) - Key: {}. Auto-switching to Database.", finalCacheKey, e);
            // Máº¡ng/Redis sáº­p -> LÆ°á»›t qua lá»—i, cháº¡y DB luÃ´n
            return joinPoint.proceed();
        }

        // 3. Cache MISS -> Thá»±c thi nghiá»‡p vá»¥ gá»‘c (Truy váº¥n Database)
        Object dbResult = joinPoint.proceed();

        // 4. LÆ°u káº¿t quáº£ vÃ o Cache náº¿u thÃ nh cÃ´ng (Bá»c try-catch phÃ²ng vá»‡)
        if (dbResult != null) {
            try {
                redisService.set(finalCacheKey, dbResult, customCache.ttl(), customCache.unit());
                log.debug("ðŸ’¾ CustomCache MISS -> STORED: key = {}, TTL = {} {}", 
                        finalCacheKey, customCache.ttl(), customCache.unit());
            } catch (Exception e) {
                log.error("âš ï¸ Redis Connection Failure (Write) - Key: {}", finalCacheKey, e);
                // Lá»—i ghi cache cÅ©ng bá» qua, khÃ´ng cháº·n luá»“ng nghiá»‡p vá»¥ tráº£ káº¿t quáº£
            }
        }

        return dbResult;
    }
}

