package com.example.filestorage.dto;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long userId,
        AuditAction action,
        Long targetId,
        String ipAddress,
        boolean success,
        String detail,
        Instant timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getTargetId(),
                log.getIpAddress(),
                log.isSuccess(),
                log.getDetail(),
                log.getTimestamp()
        );
    }
}
