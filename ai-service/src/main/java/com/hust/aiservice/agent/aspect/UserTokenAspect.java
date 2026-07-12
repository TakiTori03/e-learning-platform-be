package com.hust.aiservice.agent.aspect;

import com.hust.commonlibrary.utils.UserTokenRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Parameter;

@Aspect
@Component
@Slf4j
public class UserTokenAspect {

    @Around("@annotation(com.hust.aiservice.agent.annotation.WithUserToken)")
    public Object proceedWithToken(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        String sessionId = null;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals("sessionId") || 
                parameters[i].isAnnotationPresent(dev.langchain4j.service.MemoryId.class)) {
                if (args[i] instanceof String) {
                    sessionId = (String) args[i];
                    break;
                }
            }
        }
        
        if (sessionId == null) {
            log.warn("⚠️ Không tìm thấy sessionId trong tham số của phương thức: {}", signature.getMethod().getName());
            return joinPoint.proceed();
        }
        
        log.debug("🛡️ [AOP Token] Tự động lấy token cho session: {}", sessionId);
        String token = UserTokenRegistry.getToken(sessionId);
        if (token != null) {
            UserTokenRegistry.setCurrentToolToken(token);
        } else {
            log.warn("⚠️ Không lấy được token đăng ký cho session: {}", sessionId);
        }
        
        try {
            return joinPoint.proceed();
        } finally {
            UserTokenRegistry.clearCurrentToolToken();
            log.debug("🛡️ [AOP Token] Đã dọn dẹp token cho session: {}", sessionId);
        }
    }
}
