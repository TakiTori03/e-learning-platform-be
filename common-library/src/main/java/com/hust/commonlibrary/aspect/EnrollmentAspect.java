package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.RequireEnrollment;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import com.hust.commonlibrary.resolver.EnrollmentChecker;
import com.hust.commonlibrary.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
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
public class EnrollmentAspect {

    private final ApplicationContext applicationContext;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final java.util.Map<String, Expression> expressionCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Before("@annotation(requireEnrollment)")
    public void checkEnrollment(JoinPoint joinPoint, RequireEnrollment requireEnrollment) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        String courseId = null;

        // 1. Trích xuất context từ tham số phương thức
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
            // 2. Giải mã SpEL để lấy courseId
            if (!requireEnrollment.courseId().isBlank()) {
                Expression expression = expressionCache.computeIfAbsent(requireEnrollment.courseId(), parser::parseExpression);
                courseId = expression.getValue(context, String.class);
            }
        } catch (Exception e) {
            log.error("Enrollment AOP SpEL Parsing Error", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Nếu không có courseId, không thể tiến hành kiểm tra quyền
        if (courseId == null || courseId.isBlank()) {
            log.warn("Enrollment AOP Denied: Không tìm thấy Course ID để xác thực!");
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Truy xuất Bean Checker động dựa trên Strategy Pattern
        if (requireEnrollment.checker().isBlank()) {
            log.error("Enrollment AOP Error: Thiếu tên Bean checker được cấu hình trên Annotation!");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        try {
            EnrollmentChecker checkerBean = applicationContext.getBean(requireEnrollment.checker(), EnrollmentChecker.class);
            
            // 4. Tiến hành xác thực qua Bean nghiệp vụ cục bộ
            boolean isAllowed = checkerBean.hasAccess(currentUserId, courseId);
            
            if (!isAllowed) {
                log.warn("Enrollment AOP Access Denied: Học viên {} CHƯA ĐĂNG KÝ khóa học {}", currentUserId, courseId);
                throw new AppException(ErrorCode.UNAUTHORIZED); // Trả về 403 Forbidden
            }
            
            log.info("Enrollment AOP Granted: Chấp thuận quyền học cho Học viên {} vào khóa học {}", currentUserId, courseId);
            
        } catch (AppException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Enrollment AOP Runtime Fault: Lỗi tìm kiếm hoặc kích hoạt Checker Bean", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
