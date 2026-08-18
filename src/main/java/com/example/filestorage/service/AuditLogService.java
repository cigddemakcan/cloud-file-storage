package com.example.filestorage.service;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.AuditLog;
import com.example.filestorage.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, AuditAction action, Long targetId, String ipAddress,
                     boolean success, String detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setAction(action);
            entry.setTargetId(targetId);
            entry.setIpAddress(ipAddress);
            entry.setSuccess(success);
            entry.setDetail(detail);
            auditLogRepository.save(entry);
        } catch (Exception e) {

            log.error("Audit log kaydedilemedi: action={}, userId={}, targetId={}",
                    action, userId, targetId, e);
        }
    }
}
