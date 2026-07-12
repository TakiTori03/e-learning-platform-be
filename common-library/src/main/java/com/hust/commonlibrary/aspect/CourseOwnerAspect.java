package com.hust.commonlibrary.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.annotation.CheckCourseOwner;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.resolver.CourseIdResolver;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisOperations")
public class CourseOwnerAspect {

    private final RedisService redisService;
    private final ApplicationContext applicationContext;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final java.util.Map<String, Expression> expressionCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Before("@annotation(checkCourseOwner)")
    public void checkOwnership(JoinPoint joinPoint, CheckCourseOwner checkCourseOwner) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        String courseId = null;

        // Parse context SpEL tá»« tham sá»‘ Ä‘áº§u vÃ o cá»§a method
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
            // Ká»ŠCH Báº¢N 1: Sá»­ dá»¥ng courseId trá»±c tiáº¿p (Cho API Táº¡o má»›i)
            if (!checkCourseOwner.courseId().isBlank()) {
                Expression expression = expressionCache.computeIfAbsent(checkCourseOwner.courseId(), parser::parseExpression);
                courseId = expression.getValue(context, String.class);
            }
            // Ká»ŠCH Báº¢N 2: Sá»­ dá»¥ng domainId + Resolver (Cho API Cáº­p nháº­t/XÃ³a - Chá»‘ng hack fake ID!)
            else if (!checkCourseOwner.domainId().isBlank() && !checkCourseOwner.resolver().isBlank()) {
                Expression idExpr = expressionCache.computeIfAbsent(checkCourseOwner.domainId(), parser::parseExpression);
                String domainId = idExpr.getValue(context, String.class);

                if (domainId != null && !domainId.isBlank()) {
                    // DÃ¹ng ApplicationContext Ä‘á»ƒ lÃ´i Bean xá»‹n bÃªn Microservice kia ra query DB ngáº§m
                    CourseIdResolver resolverBean = applicationContext.getBean(checkCourseOwner.resolver(), CourseIdResolver.class);
                    courseId = resolverBean.resolveCourseId(domainId);
                }
            }
        } catch (Exception e) {
            log.error("Ultimate AOP Error: Lá»—i phÃ¢n tÃ­ch cÃº phÃ¡p phÃ¢n quyá»n", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Náº¿u khÃ´ng tÃ¬m tháº¥y courseId tÆ°Æ¡ng á»©ng, cháº·n truy cáº­p cho an toÃ n
        if (courseId == null || courseId.isBlank()) {
            log.warn("Ultimate AOP Access Denied: KhÃ´ng phÃ¢n giáº£i Ä‘Æ°á»£c Course ID há»£p lá»‡!");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // ðŸ”’ Äá»I CHIáº¾U Báº¢O Máº¬T TRÃŠN SHARED REDIS
        String key = RedisPrefixConstants.getSharedCourseOwnerKey(courseId);
        String ownerInstructorId = (String) redisService.get(key);

        if (ownerInstructorId == null || !ownerInstructorId.equals(currentUserId)) {
            log.warn("Ultimate AOP Access Denied: User {} cá»‘ gáº¯ng can thiá»‡p trÃ¡i phÃ©p khÃ³a há»c {} cá»§a {}",
                     currentUserId, courseId, ownerInstructorId);
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        log.info("Ultimate AOP Success: ÄÃ£ cho phÃ©p truy cáº­p há»£p lá»‡ cho User {} vÃ o Course {}", currentUserId, courseId);
    }
}

