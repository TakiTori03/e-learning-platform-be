package com.hust.commonlibrary.aspect;

import com.hust.commonlibrary.annotation.AuditLog;
import com.hust.commonlibrary.event.AuditLogEvent;
import com.hust.commonlibrary.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(auditLog)")
    public Object logAction(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        Instant timestamp = Instant.now();
        String currentUserId = null;
        String clientIp = "unknown";
        String targetId = null;

        // 1. Trích xuất Danh tính User
        try {
            currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        } catch (Exception e) {
            currentUserId = "anonymous";
        }

        // 2. Trích xuất Client IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            clientIp = getClientIp(request);
        }

        // 3. Trích xuất Target ID qua SpEL
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!auditLog.targetId().isBlank()) {
            try {
                Object[] args = joinPoint.getArgs();
                String[] parameterNames = signature.getParameterNames();
                
                EvaluationContext context = new StandardEvaluationContext();
                if (parameterNames != null) {
                    for (int i = 0; i < parameterNames.length; i++) {
                        context.setVariable(parameterNames[i], args[i]);
                    }
                }
                
                Expression expression = parser.parseExpression(auditLog.targetId());
                targetId = expression.getValue(context, String.class);
            } catch (Exception e) {
                log.error("AuditLog Aspect SpEL Parsing Error", e);
            }
        }

        // Tên hàm thực thi
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            // 4. Thực thi API chính của người dùng
            result = joinPoint.proceed();
            return result;

        } catch (Throwable throwable) {
            success = false;
            errorMessage = throwable.getMessage();
            throw throwable; // Ném lại exception để không làm hỏng luồng nghiệp vụ!

        } finally {
            // 5. TẠO VÀ BẮN SỰ KIỆN AUDIT LOG BẤT ĐỒNG BỘ
            try {
                AuditLogEvent auditEvent = AuditLogEvent.builder()
                        .action(auditLog.action())
                        .targetId(targetId)
                        .userId(currentUserId)
                        .clientIp(clientIp)
                        .methodName(methodName)
                        .timestamp(timestamp)
                        .success(success)
                        .errorMessage(errorMessage)
                        .description(auditLog.description())
                        .build();

                // Bắn Local Event cực nhanh (Non-blocking nếu listener dùng @Async)
                eventPublisher.publishEvent(auditEvent);
                
                log.info("AuditLog Triggered: [Action: {}] [User: {}] [Success: {}] [Target: {}]", 
                         auditLog.action(), currentUserId, success, targetId);

            } catch (Exception e) {
                // Tuyệt đối không cho phép lỗi ghi Log làm sập API chính của người dùng!
                log.error("AuditLog Aspect failed to publish event", e);
            }
        }
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
