package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.Idempotent;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
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

/**
 * Aspect Xá»­ lÃ½ Chá»‘ng trÃ¹ng láº·p yÃªu cáº§u PhÃ¢n tÃ¡n (Distributed Idempotency).
 * Sá»­ dá»¥ng Redis Atomic SETNX káº¿t há»£p vá»›i Expression Caching Ä‘áº¡t hiá»‡u nÄƒng cá»±c cao.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class IdempotentAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    // Hiá»‡u nÄƒng cá»±c Ä‘á»‰nh: Triá»‡t tiÃªu hoÃ n toÃ n chi phÃ­ CPU parse SpEL láº·p Ä‘i láº·p láº¡i
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String rawKey = null;

        // 1. TrÃ­ch xuáº¥t Key lÅ©y Ä‘áº³ng thÃ´ng qua SpEL Cache
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

            Expression expression = expressionCache.computeIfAbsent(idempotent.key(), parser::parseExpression);
            rawKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("Idempotent Aspect SpEL Parsing Error", e);
            // Gáº·p lá»—i phÃ¢n tÃ­ch cÃº phÃ¡p -> Bá» qua kiá»ƒm tra, cho phÃ©p cháº¡y hÃ m gá»‘c cho an toÃ n dá»¯ liá»‡u
            return joinPoint.proceed();
        }

        if (rawKey == null || rawKey.isBlank()) {
            return joinPoint.proceed();
        }

        // Táº¡o Ä‘á»‹nh danh ngÄƒn trÃ¹ng láº·p cho há»‡ thá»‘ng
        String idempotentKey = "idempotency:" + rawKey;

        // 2. Thao tÃ¡c KhÃ³a NguyÃªn tá»­ (Atomic SETNX) lÃªn Redis
        boolean isLocked = false;
        try {
            isLocked = redisService.setIfAbsent(
                    idempotentKey,
                    "LOCKED",
                    idempotent.expireTime(),
                    idempotent.unit()
            );
        } catch (Exception e) {
            log.error("âš ï¸ Redis Connection Error in IdempotentAspect - Key: {}. Bypassing lock checking.", idempotentKey, e);
            // Redis sáº­p -> LÆ°á»›t qua lá»—i Ä‘á»ƒ nghiá»‡p vá»¥ chÃ­nh tiáº¿p tá»¥c (High Availability)
            return joinPoint.proceed();
        }

        // 3. Kiá»ƒm tra Tráº¡ng thÃ¡i KhÃ³a
        if (!isLocked) {
            log.warn("ðŸ›‘ [Idempotency Triggered] TrÃ¹ng láº·p yÃªu cáº§u phÃ¡t hiá»‡n. KhÃ³a cháº·n: {}", idempotentKey);
            // Tráº£ lá»—i chuáº©n hÃ³a 429 (Too Many Requests) vá» phÃ­a mÃ¡y khÃ¡ch
            throw new AppException(ErrorCode.CONCURRENT_REQUEST);
        }

        // 4. Thá»±c thi logic nghiá»‡p vá»¥
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // Náº¿u tiáº¿n trÃ¬nh xáº£y ra lá»—i trÆ°á»›c khi ká»‹p hoÃ n thÃ nh, cho phÃ©p xÃ³a khÃ³a cháº·n ngay
            // Ä‘á»ƒ ngÆ°á»i dÃ¹ng cÃ³ cÆ¡ há»™i Gá»­i Láº¡i (Retry) láº­p tá»©c mÃ  khÃ´ng cáº§n chá» háº¿t TTL.
            try {
                redisService.delete(idempotentKey);
            } catch (Exception ignored) {}
            
            throw throwable;
        }
    }
}

