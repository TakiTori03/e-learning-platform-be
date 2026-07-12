package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.DistributedLock;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String rawKey = distributedLock.key();
        String resolvedKey = null;

        // 1. Parse SpEL Context tá»« Method Arguments
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();

        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        try {
            // 2. Giáº£i mÃ£ SpEL Ä‘á»ƒ sinh Lock Key cuá»‘i cÃ¹ng
            Expression expression = parser.parseExpression(rawKey);
            resolvedKey = expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("DistributedLock Aspect SpEL Parsing Error", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (resolvedKey == null || resolvedKey.isBlank()) {
            log.error("DistributedLock Aspect Error: KhÃ´ng sinh Ä‘Æ°á»£c Lock Key tá»« SpEL '{}'", rawKey);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Prefix chuáº©n cá»§a há»‡ thá»‘ng trÃ¡nh Ä‘á»¥ng hÃ ng key Redis khÃ¡c
        String finalLockKey = "elearning:lock:" + resolvedKey;
        
        log.info("DistributedLock Attempting to acquire lock on: {}", finalLockKey);
        RLock lock = redissonClient.getLock(finalLockKey);
        
        boolean isLocked = false;
        try {
            // 3. Cá»‘ gáº¯ng láº¥y khÃ³a toÃ n cá»¥c
            isLocked = lock.tryLock(
                    distributedLock.waitTime(), 
                    distributedLock.leaseTime(), 
                    distributedLock.timeUnit()
            );

            if (!isLocked) {
                // Tháº¥t báº¡i -> BÃ¡o báº­n 429
                log.warn("DistributedLock Collision Detected: KhÃ´ng thá»ƒ giá»¯ khÃ³a {} sau {}s", finalLockKey, distributedLock.waitTime());
                throw new AppException(ErrorCode.CONCURRENT_REQUEST);
            }

            log.info("DistributedLock SUCCESSFULLY ACQUIRED lock: {}", finalLockKey);
            
            // 4. Tiáº¿n hÃ nh thá»±c thi Business Logic thá»±c sá»±!
            return joinPoint.proceed();

        } finally {
            // 5. Äáº¢M Báº¢O 100%: Chá»‰ má»Ÿ khÃ³a náº¿u luá»“ng hiá»‡n táº¡i Ä‘ang náº¯m giá»¯ khÃ³a (chá»‘ng crash hoáº·c nháº§m luá»“ng)
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("DistributedLock SUCCESSFULLY RELEASED lock: {}", finalLockKey);
            }
        }
    }
}

