package com.example.filestorage.aspect;

import com.example.filestorage.annotation.Auditable;
import com.example.filestorage.security.CustomUserDetails;
import com.example.filestorage.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logSuccess(JoinPoint joinPoint, Auditable auditable, Object result) {
        Long userId = resolveUserId();
        Long targetId = resolveTargetId(joinPoint, result);
        String ip = resolveIp();

        auditLogService.log(userId, auditable.action(), targetId, ip, true, null);
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "exception")
    public void logFailure(JoinPoint joinPoint, Auditable auditable, Exception exception) {
        Long userId = resolveUserId();
        Long targetId = resolveTargetId(joinPoint, null);
        String ip = resolveIp();

        String detail = exception.getMessage();
        if (detail != null && detail.length() > 500) {
            detail = detail.substring(0, 500);
        }

        auditLogService.log(userId, auditable.action(), targetId, ip, false, detail);
    }

    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        return null;
    }


    private Long resolveTargetId(JoinPoint joinPoint, Object result) {
        if (result != null) {
            Long idFromReturn = tryExtractId(result);
            if (idFromReturn != null) {
                return idFromReturn;
            }
        }

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long longArg) {
                return longArg;
            }
        }
        return null;
    }

    private Long tryExtractId(Object obj) {
        try {
            Method getId = obj.getClass().getMethod("getId");
            Object id = getId.invoke(obj);
            return id instanceof Long ? (Long) id : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
